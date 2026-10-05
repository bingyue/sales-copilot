package cn.iyque.sales.service;

import cn.iyque.dao.IYqueMsgAuditDao;
import cn.iyque.sales.domain.SalesActivity;
import cn.iyque.sales.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SalesArchiveImport {
    private final SalesService sales;
    private final SalesLeadRepository leads;
    private final SalesActivityRepository activities;
    private final IYqueMsgAuditDao messages;
    @Transactional
    public int importStored(String leadId) {
        var lead=sales.lock(leadId);
        if(lead.getCustomerKey()==null) return 0;
        int imported=0;
        while(true) {
            var page=messages.findSalesConversation(lead.getExternalUserId(),lead.getOwnerUserId(),lead.getArchiveSequence(),PageRequest.of(0,500));
            if(page.isEmpty()) break;
            for(var m:page) {
                String ref="archive:"+leadId+":"+m.getMsgId();
                if(!activities.existsBySourceRef(ref)) {
                    var a=new SalesActivity(); a.setId(SalesService.id()); a.setLeadId(leadId); a.setType("ARCHIVE_MESSAGE");
                    a.setRole(lead.getExternalUserId().equals(m.getFromId())?"CUSTOMER":"SELLER");
                    String content=m.getContent()==null?"":m.getContent();
                    a.setContent(content.length()>12000?content.substring(0,12000):content); a.setSourceRef(ref); a.setActor("wecom-archive");
                    a.setOccurredAt(m.getMsgTime().toInstant()); a.setCreatedAt(Instant.now()); activities.save(a); imported++;
                }
                lead.setArchiveSequence(m.getDataSeq());
            }
            if(page.size()<500) break;
        }
        if(imported>0) sales.touch(lead);
        leads.save(lead); return imported;
    }
}
