package cn.iyque.sales.repository;

import cn.iyque.sales.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface SalesSkillRunRepository extends JpaRepository<SalesSkillRun,String> {
    Optional<SalesSkillRun> findByRequestKey(String requestKey);
    List<SalesSkillRun> findByLeadIdOrderByCreatedAtDesc(String leadId, Pageable pageable);
    @Modifying
    @Query("update SalesSkillRun r set r.status = 'FAILED', r.errorCode = 'INTERRUPTED', r.finishedAt = :now where r.status = 'RUNNING' and r.createdAt < :cutoff")
    int expireRunning(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
