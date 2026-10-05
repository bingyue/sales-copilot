package cn.iyque.sales.skill;

import cn.iyque.sales.repository.*;
import cn.iyque.sales.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesContextService {
    private final SalesService sales;
    private final SalesActivityRepository activities;
    private final SalesFollowupTaskRepository tasks;
    private final SalesCatalog catalog;
    @Transactional(readOnly=true)
    public Map<String,Object> context(String leadId) {
        var lead=sales.get(leadId);
        List<Map<String,Object>> conversation=new ArrayList<>(); int remaining=24000;
        var latest=activities.findByLeadIdOrderByOccurredAtDesc(leadId,PageRequest.of(0,100));
        for(var a:latest) {
            if(!Set.of("CONVERSATION","ARCHIVE_MESSAGE").contains(a.getType())) continue;
            String content=a.getContent(); if(content.length()>remaining) content=content.substring(0,remaining);
            conversation.add(Map.of("id",a.getId(),"role",a.getRole(),"content",content,"time",a.getOccurredAt()));
            remaining-=content.length(); if(remaining<=0) break;
        }
        Collections.reverse(conversation);
        return Map.of("now",Instant.now(),"timezone","Asia/Shanghai","lead",lead,"profileEvidenceId","profile:"+leadId,
                "conversation",conversation,"pendingTasks",tasks.findByLeadIdAndStatus(leadId,"PENDING"),
                "products",catalog.all(),"catalogVersion",catalog.version(),"sourceNotice","只包含已录入或已同步的内容；缺失消息不代表客户没有回复。");
    }
}
