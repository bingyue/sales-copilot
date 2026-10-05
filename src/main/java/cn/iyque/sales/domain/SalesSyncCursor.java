package cn.iyque.sales.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name="iyque_sales_sync_cursor")
public class SalesSyncCursor {
    @Id private String id;
    private long sequence;
    @Version private long version;
    @Column(name="last_attempt") private Instant lastAttempt;
    @Column(name="last_error",length=120) private String lastError;
}
