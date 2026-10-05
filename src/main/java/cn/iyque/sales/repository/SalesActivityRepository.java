package cn.iyque.sales.repository;

import cn.iyque.sales.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface SalesActivityRepository extends JpaRepository<SalesActivity,String> {
    List<SalesActivity> findByLeadIdOrderByOccurredAtDesc(String leadId, Pageable pageable);
    boolean existsBySourceRef(String sourceRef);
}
