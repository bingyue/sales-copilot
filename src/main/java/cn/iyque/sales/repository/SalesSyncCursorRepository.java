package cn.iyque.sales.repository;

import cn.iyque.sales.domain.SalesSyncCursor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.Optional;

public interface SalesSyncCursorRepository extends JpaRepository<SalesSyncCursor,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SalesSyncCursor s where s.id = :id")
    Optional<SalesSyncCursor> lockById(@Param("id") String id);
}
