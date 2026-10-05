package cn.iyque.sales;

import cn.iyque.dao.IYqueMsgAuditDao;
import cn.iyque.sales.domain.SalesSyncCursor;
import cn.iyque.sales.repository.SalesSyncCursorRepository;
import cn.iyque.sales.service.ArchiveSyncService;
import cn.iyque.service.IYqueConfigService;
import me.chanjar.weixin.cp.api.*;
import me.chanjar.weixin.cp.bean.msgaudit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArchiveSyncServiceTest {
    IYqueConfigService config=mock(IYqueConfigService.class);
    IYqueMsgAuditDao messages=mock(IYqueMsgAuditDao.class);
    SalesSyncCursorRepository cursors=mock(SalesSyncCursorRepository.class);
    TransactionTemplate tx=mock(TransactionTemplate.class);
    WxCpService cp=mock(WxCpService.class);
    WxCpMsgAuditService api=mock(WxCpMsgAuditService.class);
    SalesSyncCursor cursor=new SalesSyncCursor();
    ArchiveSyncService sync=new ArchiveSyncService(config,messages,cursors,tx);
    @BeforeEach void setup() throws Exception {
        cursor.setId("wecom-archive"); cursor.setSequence(10);
        when(cursors.existsById(anyString())).thenReturn(true);
        when(cursors.findById(anyString())).thenReturn(Optional.of(cursor));
        when(cursors.lockById(anyString())).thenReturn(Optional.of(cursor));
        when(config.findWxcpservice()).thenReturn(cp); when(cp.getMsgAuditService()).thenReturn(api);
        doAnswer(inv->{((Consumer<TransactionStatus>)inv.getArgument(0)).accept(mock(TransactionStatus.class)); return null;}).when(tx).executeWithoutResult(any());
        when(api.getDecryptData(anyLong(),any(),anyInt())).thenAnswer(inv->{
            var item=(WxCpChatDatas.WxCpChatData)inv.getArgument(1);
            var m=new WxCpChatModel(); m.setMsgId(item.getMsgId()); m.setFrom("customer"); m.setTolist(new String[]{"employee"}); m.setMsgType("text"); m.setMsgTime(1700000000000L); return m;
        });
    }
    WxCpChatDatas page(int count,long first) {
        var p=new WxCpChatDatas(); p.setSdk(1L); p.setErrCode(0); var items=new ArrayList<WxCpChatDatas.WxCpChatData>();
        for(int i=0;i<count;i++) {var m=new WxCpChatDatas.WxCpChatData(); m.setSeq(first+i); m.setMsgId("message-"+(first+i)); m.setPublickeyVer(7); items.add(m);}
        p.setChatData(items); return p;
    }
    @Test void pageIsCheckpointedAndNextPageIsFetched() throws Exception {
        when(api.getChatDatas(10,1000,null,null,30)).thenReturn(page(1000,11));
        when(api.getChatDatas(1010,1000,null,null,30)).thenReturn(page(1,1011));
        sync.sync(); assertThat(cursor.getSequence()).isEqualTo(1011); assertThat(cursor.getLastError()).isNull();
        verify(messages,times(2)).saveAllAndFlush(any()); verify(api,times(1001)).getDecryptData(eq(1L),any(),eq(7));
    }
    @Test void failedDecodeDoesNotSavePartialPageOrAdvanceCursor() throws Exception {
        when(api.getChatDatas(10,1000,null,null,30)).thenReturn(page(2,11));
        doThrow(new IllegalStateException("decode failed")).when(api).getDecryptData(eq(1L),argThat(v->v!=null&&v.getSeq()==12),eq(7));
        sync.sync(); assertThat(cursor.getSequence()).isEqualTo(10); assertThat(cursor.getLastError()).isNotNull();
        verify(messages,never()).saveAllAndFlush(any());
    }
    @Test void apiFailureIsNotMistakenForEmptySuccess() throws Exception {
        var p=page(0,11); p.setErrCode(40001); when(api.getChatDatas(10,1000,null,null,30)).thenReturn(p);
        sync.sync(); assertThat(cursor.getSequence()).isEqualTo(10); assertThat(cursor.getLastError()).isNotNull();
    }
    @Test void retryResumesFromLastSuccessfulPage() throws Exception {
        when(api.getChatDatas(10,1000,null,null,30)).thenReturn(page(1000,11));
        when(api.getChatDatas(1010,1000,null,null,30)).thenThrow(new IllegalStateException("network"));
        sync.sync(); assertThat(cursor.getSequence()).isEqualTo(1010);
        doReturn(page(1,1011)).when(api).getChatDatas(1010,1000,null,null,30);
        sync.sync(); assertThat(cursor.getSequence()).isEqualTo(1011); assertThat(cursor.getLastError()).isNull();
        verify(api,times(1)).getChatDatas(10,1000,null,null,30);
    }
}
