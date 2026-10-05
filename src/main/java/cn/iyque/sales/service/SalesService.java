package cn.iyque.sales.service;

import cn.iyque.dao.IYQueCustomerInfoDao;
import cn.iyque.sales.domain.*;
import cn.iyque.sales.dto.SalesRequests.*;
import cn.iyque.sales.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static cn.iyque.sales.service.SalesRules.*;

@Service
@RequiredArgsConstructor
public class SalesService {
    private final SalesLeadRepository leads;
    private final SalesActivityRepository activities;
    private final SalesFollowupTaskRepository tasks;
    private final SalesReceiptRepository receipts;
    private final SalesSkillRunRepository runs;
    private final IYQueCustomerInfoDao customers;
    private final SalesCatalog catalog;
    private final ObjectMapper mapper;
    @javax.persistence.PersistenceContext
    private javax.persistence.EntityManager entityManager;

    public static String id() { return UUID.randomUUID().toString(); }
    public SalesLead get(String id) { return leads.findById(id).orElseThrow(SalesRules::missing); }
    public SalesLead lock(String id) { return leads.lockById(id).orElseThrow(SalesRules::missing); }

    @Transactional
    public SalesLead create(CreateLead input, String actor) {
        String customerKey = text(input.customerKey(), 240, "客户关系标识", false);
        if (!customerKey.isEmpty()) {
            var old = leads.findByCustomerKey(customerKey);
            if (old.isPresent()) return old.get();
        }
        SalesLead lead = new SalesLead();
        lead.setId(id()); lead.setName(text(input.name(), 120, "客户名称", customerKey.isEmpty()));
        lead.setSource(text(input.source(), 80, "来源", false));
        lead.setOwnerUserId(text(input.ownerUserId(), 120, "负责人", false));
        if (!customerKey.isEmpty()) {
            int separator = customerKey.lastIndexOf('&');
            if (separator <= 0 || separator == customerKey.length()-1) throw bad("客户关系标识无效");
            var customer = customers.findByExternalUseridAndUserId(customerKey.substring(0, separator), customerKey.substring(separator+1));
            if (customer == null || !customerKey.equals(customer.getEId())) throw missing();
            lead.setCustomerKey(customerKey); lead.setExternalUserId(customer.getExternalUserid());
            lead.setOwnerUserId(customer.getUserId()); lead.setName(text(customer.getCustomerName(),120,"客户名称",true));
        }
        lead.setCreatedAt(Instant.now()); lead.setUpdatedAt(lead.getCreatedAt());
        lead = leads.saveAndFlush(lead);
        event(lead, "CREATED", "建立销售档案", actor);
        return leads.saveAndFlush(lead);
    }

    @Transactional(readOnly=true)
    public Page<SalesLead> list(String query, String stage, int page, int size) {
        Specification<SalesLead> spec = (r,q,b) -> b.conjunction();
        if (query != null && !query.isBlank()) {
            String word = "%" + query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r,q,b) -> b.like(r.get("name"), word, '\\'));
        }
        if (stage != null && !stage.isBlank()) spec = spec.and((r,q,b) -> b.equal(r.get("stage"), stage));
        return leads.findAll(spec, PageRequest.of(Math.max(0,page), Math.min(100,Math.max(1,size)),Sort.by(Sort.Direction.DESC,"updatedAt")));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> detail(String id) {
        return Map.of("lead", get(id), "activities", activities.findByLeadIdOrderByOccurredAtDesc(id,PageRequest.of(0,200)),
                "tasks", tasks.findByLeadIdOrderByDueAtAsc(id), "receipts", receipts.findByLeadIdOrderByPaidAtDesc(id),
                "runs", runs.findByLeadIdOrderByCreatedAtDesc(id,PageRequest.of(0,20)));
    }

    @Transactional
    public SalesLead update(String id, UpdateLead input, String actor) {
        SalesLead lead = lock(id); version(lead.getVersion(),input.version());
        if (!STAGES.contains(input.stage())) throw bad("销售阶段无效");
        boolean paid = receipts.existsByLeadIdAndStatus(id,"CONFIRMED");
        if ("WON".equals(input.stage()) != paid) throw bad("已成交状态须与有效实收记录一致");
        catalog.validateId(input.productId());
        lead.setName(text(input.name(),120,"客户名称",true));
        lead.setSource(text(input.source(),80,"来源",false));
        lead.setNeeds(text(input.needs(),4000,"需求",false));
        lead.setConcerns(text(input.concerns(),4000,"顾虑",false));
        lead.setProductId(input.productId()); lead.setStage(input.stage()); lead.setStopFollowup(input.stopFollowup());
        if (lead.isStopFollowup() || Set.of("WON","LOST").contains(lead.getStage())) cancelPending(id);
        event(lead,"PROFILE_UPDATED","更新客户档案，阶段：" + lead.getStage(),actor);
        return leads.saveAndFlush(lead);
    }

    @Transactional
    public SalesActivity addActivity(String id, AddActivity input, String actor) {
        SalesLead lead = lock(id);
        String source = "manual:" + id + ":" + key(input.requestKey());
        if (activities.existsBySourceRef(source)) return null;
        if (!Set.of("CUSTOMER","SELLER","NOTE").contains(input.role())) throw bad("发言角色无效");
        Instant time = input.occurredAt() == null ? Instant.now() : input.occurredAt();
        if (time.isAfter(Instant.now().plusSeconds(60))) throw bad("沟通时间不能晚于现在");
        SalesActivity activity = new SalesActivity();
        activity.setId(id()); activity.setLeadId(id); activity.setType("CONVERSATION"); activity.setRole(input.role());
        activity.setContent(text(input.content(),12000,"对话内容",true)); activity.setOccurredAt(time);
        activity.setCreatedAt(Instant.now()); activity.setActor(actor); activity.setSourceRef(source);
        touch(lead); leads.save(lead);
        return activities.save(activity);
    }

    @Transactional
    public SalesFollowupTask createTask(CreateTask input, String actor) {
        SalesLead lead = lock(input.leadId());
        String requestKey = "task:" + key(input.requestKey());
        var existing = tasks.findByRequestKey(requestKey);
        if (existing.isPresent()) { sameLead(existing.get().getLeadId(),lead.getId()); return existing.get(); }
        canFollow(lead); due(input.dueAt());
        var task = task(lead,input.action(),input.reason(),input.dueAt(),requestKey,null);
        event(lead,"TASK_CREATED","安排跟进："+task.getAction(),actor);
        return task;
    }

    private SalesFollowupTask task(SalesLead lead, String action, String reason, Instant dueAt, String requestKey, String runId) {
        SalesFollowupTask task = new SalesFollowupTask();
        task.setId(id()); task.setLeadId(lead.getId()); task.setAction(text(action,1000,"跟进动作",true));
        task.setReason(text(reason,2000,"跟进原因",false)); task.setDueAt(dueAt); task.setRequestKey(requestKey);
        task.setRunId(runId); task.setCreatedAt(Instant.now());
        return tasks.saveAndFlush(task);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> listTasks(String status, String scope, int page, int size) {
        Specification<SalesFollowupTask> spec = (r,q,b) -> b.conjunction();
        if (status != null && !status.isBlank()) spec = spec.and((r,q,b) -> b.equal(r.get("status"),status));
        Instant now=Instant.now();
        Instant tomorrow=LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        if ("overdue".equals(scope)) spec=spec.and((r,q,b)->b.lessThan(r.get("dueAt"),now));
        if ("today".equals(scope)) spec=spec.and((r,q,b)->b.lessThan(r.get("dueAt"),tomorrow));
        var data=tasks.findAll(spec,PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by("dueAt")));
        List<Map<String,Object>> rows=new ArrayList<>();
        for (var t:data) rows.add(Map.of("task",t,"leadName",get(t.getLeadId()).getName()));
        return Map.of("content",rows,"totalElements",data.getTotalElements());
    }

    @Transactional
    public SalesFollowupTask actOnTask(String id,String action,TaskAction input,String actor) {
        SalesFollowupTask initial=tasks.findById(id).orElseThrow(SalesRules::missing);
        SalesLead lead=lock(initial.getLeadId());
        // Refresh under the lead lock; task version rejects stale browser operations.
        SalesFollowupTask task=tasks.findById(id).orElseThrow(SalesRules::missing);
        entityManager.refresh(task);
        if (!"PENDING".equals(task.getStatus())) {
            if (("complete".equals(action)&&"DONE".equals(task.getStatus())) || ("cancel".equals(action)&&"CANCELLED".equals(task.getStatus()))) return task;
            throw bad("任务已结束");
        }
        version(task.getVersion(),input.version());
        switch(action) {
            case "complete": canFollow(lead); task.setResultNote(text(input.resultNote(),4000,"跟进结果",true)); task.setStatus("DONE"); task.setCompletedAt(Instant.now()); break;
            case "postpone": canFollow(lead); due(input.dueAt()); task.setDueAt(input.dueAt()); break;
            case "cancel": task.setStatus("CANCELLED"); break;
            default: throw bad("任务操作无效");
        }
        event(lead,"TASK_"+action.toUpperCase(Locale.ROOT),task.getAction()+(task.getResultNote()==null?"":"；结果："+task.getResultNote()),actor);
        return tasks.saveAndFlush(task);
    }

    @Transactional
    public SalesReceipt receipt(String id,CreateReceipt input,String actor) {
        SalesLead lead=lock(id);
        String requestKey="receipt:"+key(input.requestKey());
        var old=receipts.findByRequestKey(requestKey);
        if(old.isPresent()) { sameLead(old.get().getLeadId(),id); return old.get(); }
        if(input.amount()==null||input.amount().signum()<=0||input.amount().scale()>2||input.amount().compareTo(new BigDecimal("999999999999.99"))>0) throw bad("实收金额应大于零且最多两位小数");
        if(input.paidAt()==null||input.paidAt().isAfter(Instant.now().plusSeconds(60))) throw bad("收款时间无效");
        catalog.validateId(input.productId());
        String ref=text(input.receiptRef(),180,"凭证号",false);
        if(!ref.isBlank()&&receipts.existsByReceiptRef(ref)) throw bad("该凭证号已登记");
        SalesReceipt r=new SalesReceipt(); r.setId(id()); r.setLeadId(id); r.setProductId(input.productId());
        r.setAmount(input.amount()); r.setPaidAt(input.paidAt()); r.setRequestKey(requestKey); r.setReceiptRef(ref.isBlank()?null:ref);
        r.setActor(actor); r.setCreatedAt(Instant.now()); receipts.saveAndFlush(r);
        lead.setStage("WON"); cancelPending(id); event(lead,"RECEIPT_CONFIRMED","登记实收 ¥"+r.getAmount().toPlainString(),actor);
        return r;
    }

    @Transactional
    public SalesReceipt voidReceipt(String id,VoidReceipt input,String actor) {
        SalesReceipt r=receipts.findById(id).orElseThrow(SalesRules::missing);
        SalesLead lead=lock(r.getLeadId());
        entityManager.refresh(r);
        if("VOIDED".equals(r.getStatus())) return r;
        r.setVoidReason(text(input.reason(),1000,"作废原因",true)); r.setStatus("VOIDED"); r.setVoidedAt(Instant.now());
        receipts.saveAndFlush(r);
        if(!receipts.existsByLeadIdAndStatus(lead.getId(),"CONFIRMED")) {
            if(input.stage()==null||!STAGES.contains(input.stage())||"WON".equals(input.stage())) throw bad("请确认作废后的非成交阶段");
            lead.setStage(input.stage());
        }
        event(lead,"RECEIPT_VOIDED","作废实收 ¥"+r.getAmount().toPlainString()+"；原因："+r.getVoidReason(),actor);
        return r;
    }

    @Transactional
    public SalesSkillRun apply(String runId,ApplySkill input,String actor) throws Exception {
        SalesSkillRun run=runs.findById(runId).orElseThrow(SalesRules::missing);
        SalesLead lead=lock(run.getLeadId());
        entityManager.refresh(run);
        if(run.getAppliedAt()!=null) return run;
        if(!"SUCCEEDED".equals(run.getStatus())) throw bad("仅可采纳成功生成的建议");
        version(lead.getContextVersion(),input.contextVersion()); version(lead.getContextVersion(),run.getContextVersion());
        if(run.getInputJson()==null || !java.util.Objects.equals(mapper.readTree(run.getInputJson()).path("catalogVersion").asText(),catalog.version())) throw bad("产品目录已变化，请重新生成建议");
        JsonNode output=mapper.readTree(run.getOutputJson());
        switch(run.getSkillId()) {
            case "analyze-lead":
                lead.setNeeds(output.path("needs").asText()); lead.setConcerns(output.path("concerns").asText());
                String product=output.path("productId").asText(); catalog.validateId(product); lead.setProductId(product);
                // Stage remains a human choice; AI analysis never sets WON or overrides a stop decision.
                break;
            case "plan-followup":
                canFollow(lead);
                if(!tasks.findByLeadIdAndStatus(lead.getId(),"PENDING").isEmpty()) throw bad("已有待办，请先处理或取消，避免重复跟进");
                Instant dueAt=Instant.parse(output.path("dueAt").asText()); due(dueAt);
                task(lead,output.path("action").asText(),output.path("reason").asText(),dueAt,"skill:"+runId,runId);
                break;
            case "suggest-reply": break;
            default: throw bad("未知 Skill");
        }
        run.setAppliedAt(Instant.now());
        event(lead,"AI_APPLIED","采纳 AI 建议："+run.getSkillId()+"（不代表消息已发送）",actor);
        return runs.save(run);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> metrics() {
        var zone=ZoneId.of("Asia/Shanghai");
        Instant start=LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant end=LocalDate.now(zone).withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant();
        long newLeads=leads.count((r,q,b)->b.and(b.greaterThanOrEqualTo(r.get("createdAt"),start),b.lessThan(r.get("createdAt"),end)));
        Instant tomorrow=LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();
        return Map.of("totalLeads",leads.count(),"newLeadsThisMonth",newLeads,"wonLeads",receipts.countWonLeads(),
                "revenueThisMonth",receipts.sumConfirmed(start,end),"dueToday",tasks.countByStatusAndDueAtBefore("PENDING",tomorrow),
                "overdue",tasks.countByStatusAndDueAtBefore("PENDING",Instant.now()));
    }

    private void cancelPending(String id) {
        for(var t:tasks.findByLeadIdAndStatus(id,"PENDING")) { t.setStatus("CANCELLED"); t.setResultNote("客户成交或停止推进，系统取消转化任务"); tasks.save(t); }
    }
    private void sameLead(String actual,String expected) { if(!actual.equals(expected)) throw bad("请求标识已被其他客户使用"); }
    public void touch(SalesLead lead) { lead.setContextVersion(lead.getContextVersion()+1); lead.setUpdatedAt(Instant.now()); }
    public void event(SalesLead lead,String type,String content,String actor) {
        SalesActivity a=new SalesActivity(); a.setId(id()); a.setLeadId(lead.getId()); a.setType(type); a.setRole("SYSTEM");
        a.setContent(content); a.setActor(actor); a.setOccurredAt(Instant.now()); a.setCreatedAt(a.getOccurredAt()); activities.save(a);
        touch(lead); leads.save(lead);
    }
}
