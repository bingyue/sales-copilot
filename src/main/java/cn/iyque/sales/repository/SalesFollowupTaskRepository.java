package cn.iyque.sales.repository;

import cn.iyque.sales.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface SalesFollowupTaskRepository extends JpaRepository<SalesFollowupTask,String>, JpaSpecificationExecutor<SalesFollowupTask> {
    List<SalesFollowupTask> findByLeadIdOrderByDueAtAsc(String leadId);
    List<SalesFollowupTask> findByLeadIdAndStatus(String leadId, String status);
    Optional<SalesFollowupTask> findByRequestKey(String requestKey);
    long countByStatusAndDueAtBefore(String status, Instant time);
}
