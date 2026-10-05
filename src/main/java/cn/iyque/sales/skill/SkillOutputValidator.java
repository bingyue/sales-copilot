package cn.iyque.sales.skill;

import cn.iyque.sales.service.SalesCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import static cn.iyque.sales.service.SalesRules.*;

@Component
@RequiredArgsConstructor
public class SkillOutputValidator {
    private final ObjectMapper mapper;
    private final SalesCatalog catalog;
    public JsonNode validate(String skill,String raw,Map<String,Object> context) throws Exception {
        if(raw==null||raw.length()>20000) throw bad("AI 输出为空或过长");
        String json=raw.trim(); if(json.startsWith("```")) json=json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        JsonNode node=mapper.readTree(json);
        if(node==null||!node.isObject()) throw bad("AI 输出必须是 JSON 对象");
        Set<String> fields=switch(skill) {
            case "analyze-lead" -> Set.of("needs","concerns","productId","stageSuggestion","missingInfo","evidenceIds");
            case "suggest-reply" -> Set.of("reply","materialIds","questions","evidenceIds");
            case "plan-followup" -> Set.of("action","reason","dueAt","evidenceIds");
            default -> throw bad("未知 Skill");
        };
        node.fieldNames().forEachRemaining(k->{if(!fields.contains(k)) throw bad("AI 输出包含未知字段");});
        for(String f:fields) if(!node.has(f)) throw bad("AI 输出缺少字段："+f);
        JsonNode ctx=mapper.valueToTree(context);
        Set<String> evidence=new HashSet<>(); evidence.add(ctx.path("profileEvidenceId").asText());
        ctx.path("conversation").forEach(n->evidence.add(n.path("id").asText()));
        List<String> cited=array(node,"evidenceIds",20,160);
        if(cited.isEmpty()||!evidence.containsAll(cited)) throw bad("AI 建议缺少有效依据");
        switch(skill) {
            case "analyze-lead":
                string(node,"needs",4000,false); string(node,"concerns",4000,false);
                catalog.validateId(string(node,"productId",80,false));
                if(!Set.of("NEW","CONTACTED","QUALIFIED","OFFERED","LOST","UNKNOWN").contains(string(node,"stageSuggestion",32,true))) throw bad("AI 销售阶段无效");
                array(node,"missingInfo",10,500); break;
            case "suggest-reply":
                String reply=string(node,"reply",4000,true);
                if(Pattern.compile("(?:https?://|www\\.)",Pattern.CASE_INSENSITIVE).matcher(reply).find()) throw bad("资料链接必须通过目录引用，不能自由生成");
                // Prices are rendered separately from the configured catalog, never from generated prose.
                if(Pattern.compile("[¥￥$]|[0-9零一二三四五六七八九十百千万两]+(?:\\.[0-9]+)?\\s*(?:元|块|折|人民币|美元)").matcher(reply).find()) throw bad("报价须引用产品目录，请勿在草稿中自由生成金额");
                Set<String> materialIds=new HashSet<>();
                for(var p:catalog.all()) if(p.materials()!=null) for(var m:p.materials()) materialIds.add(m.id());
                if(!materialIds.containsAll(array(node,"materialIds",8,100))) throw bad("引用资料不在有效目录中");
                array(node,"questions",10,500); break;
            case "plan-followup":
                string(node,"action",1000,true); string(node,"reason",2000,true);
                due(Instant.parse(string(node,"dueAt",40,true))); break;
        }
        return node;
    }
    private String string(JsonNode node,String field,int max,boolean required) {
        if(!node.path(field).isTextual()) throw bad("AI 字段类型错误："+field);
        return text(node.path(field).asText(),max,field,required);
    }
    private List<String> array(JsonNode node,String field,int max,int itemMax) {
        JsonNode a=node.path(field); if(!a.isArray()||a.size()>max) throw bad("AI 数组格式错误："+field);
        List<String> list=new ArrayList<>(); for(JsonNode n:a) { if(!n.isTextual()) throw bad("AI 数组项类型错误"); list.add(text(n.asText(),itemMax,field,true)); }
        return list;
    }
}
