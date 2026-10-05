package cn.iyque.sales.service;

import cn.iyque.dao.IYqueMsgAuditDao;
import cn.iyque.entity.IYqueMsgAudit;
import cn.iyque.sales.domain.SalesSyncCursor;
import cn.iyque.sales.repository.SalesSyncCursorRepository;
import cn.iyque.service.IYqueConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;

/** Durable page checkpoints: a failure never advances past an unprocessed message. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveSyncService {
    private final IYqueConfigService config;
    private final IYqueMsgAuditDao messages;
    private final SalesSyncCursorRepository cursors;
    private final TransactionTemplate transactions;
    @Value("${sales.archive.enabled:false}") private boolean enabled;
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void scheduled() { if(enabled) sync(); }

    public synchronized void sync() {
        String stream="wecom-archive";
        if(!cursors.existsById(stream)) { var c=new SalesSyncCursor(); c.setId(stream); cursors.saveAndFlush(c); }
        try {
            var service=config.findWxcpservice().getMsgAuditService();
            for(int pageNumber=0;pageNumber<100;pageNumber++) {
                long seq=cursors.findById(stream).orElseThrow().getSequence();
                var page=service.getChatDatas(seq,1000L,null,null,30L);
                if(page==null||page.getErrCode()!=0) throw new IllegalStateException("ARCHIVE_API_ERROR");
                if(page.getChatData()==null||page.getChatData().isEmpty()) return;
                List<IYqueMsgAudit> decoded=new ArrayList<>(); long checkpoint=seq;
                for(var item:page.getChatData()) {
                    var data=service.getDecryptData(page.getSdk(),item,item.getPublickeyVer());
                    if(data==null) throw new IllegalStateException("EMPTY_DECRYPTED_MESSAGE");
                    boolean group=data.getRoomId()!=null&&!data.getRoomId().isBlank();
                    String recipient=group?data.getRoomId():(data.getTolist()==null||data.getTolist().length==0?"":data.getTolist()[0]);
                    String content="text".equals(data.getMsgType())&&data.getText()!=null?data.getText().getContent():"[非文字消息，未用于销售分析]";
                    decoded.add(IYqueMsgAudit.builder().msgId(data.getMsgId()).fromId(data.getFrom()).fromName(data.getFrom())
                            .acceptId(recipient).acceptName(recipient).acceptType(group?2:1).msgType(data.getMsgType())
                            .content(content).dataSeq(item.getSeq()).msgTime(new Date(data.getMsgTime())).createTime(new Date()).build());
                    checkpoint=Math.max(checkpoint,item.getSeq());
                }
                if(checkpoint<=seq) throw new IllegalStateException("CURSOR_DID_NOT_ADVANCE");
                long next=checkpoint;
                transactions.executeWithoutResult(tx->{
                    var state=cursors.lockById(stream).orElseThrow();
                    if(state.getSequence()!=seq) throw new IllegalStateException("CONCURRENT_SYNC");
                    messages.saveAllAndFlush(decoded);
                    state.setSequence(next); state.setLastAttempt(Instant.now()); state.setLastError(null); cursors.saveAndFlush(state);
                });
                if(page.getChatData().size()<1000) return;
            }
        } catch(Exception e) {
            transactions.executeWithoutResult(tx->{var state=cursors.lockById(stream).orElseThrow(); state.setLastAttempt(Instant.now()); state.setLastError("SYNC_FAILED_RETRY_FROM_CHECKPOINT"); cursors.save(state);});
            log.warn("企微会话同步未完成，保留检查点等待重试。异常类型: {}",e.getClass().getSimpleName());
        }
    }
}
