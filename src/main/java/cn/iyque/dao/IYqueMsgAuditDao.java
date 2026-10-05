package cn.iyque.dao;

import cn.iyque.entity.IYQueComplain;
import cn.iyque.entity.IYqueMsgAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;

public interface IYqueMsgAuditDao extends JpaRepository<IYqueMsgAudit,String> , JpaSpecificationExecutor<IYqueMsgAudit> {

    @org.springframework.data.jpa.repository.Query("select m from iyque_msg_audit m where m.acceptType = 1 and m.msgType = 'text' and m.dataSeq > :seq and ((m.fromId = :customer and m.acceptId = :employee) or (m.fromId = :employee and m.acceptId = :customer)) order by m.dataSeq asc")
    List<IYqueMsgAudit> findSalesConversation(@org.springframework.data.repository.query.Param("customer") String customer,
            @org.springframework.data.repository.query.Param("employee") String employee,
            @org.springframework.data.repository.query.Param("seq") long seq, org.springframework.data.domain.Pageable pageable);

    /**
     * 获取最新的分页下标
     * @return
     */
    IYqueMsgAudit findTopByOrderByDataSeqDesc();


    /**
     * 查询当天凌晨到当前时间的数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param acceptType
     * @return 符合条件的消息列表
     */
    List<IYqueMsgAudit> findByMsgTimeBetweenAndAcceptType(Date startTime, Date endTime,Integer acceptType);

}
