package cn.iyque.sales.repository;

import cn.iyque.sales.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface SalesReceiptRepository extends JpaRepository<SalesReceipt,String> {
    List<SalesReceipt> findByLeadIdOrderByPaidAtDesc(String leadId);
    Optional<SalesReceipt> findByRequestKey(String requestKey);
    boolean existsByReceiptRef(String receiptRef);
    boolean existsByLeadIdAndStatus(String leadId, String status);
    @Query("select coalesce(sum(r.amount),0) from SalesReceipt r where r.status = 'CONFIRMED' and r.paidAt >= :start and r.paidAt < :end")
    java.math.BigDecimal sumConfirmed(@Param("start") Instant start, @Param("end") Instant end);
    @Query("select count(distinct r.leadId) from SalesReceipt r where r.status = 'CONFIRMED'")
    long countWonLeads();
}
