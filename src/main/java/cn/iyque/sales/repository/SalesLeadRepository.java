package cn.iyque.sales.repository;

import cn.iyque.sales.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface SalesLeadRepository extends JpaRepository<SalesLead,String>, JpaSpecificationExecutor<SalesLead> {
    Optional<SalesLead> findByCustomerKey(String customerKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SalesLead l where l.id = :id")
    Optional<SalesLead> lockById(@Param("id") String id);
}
