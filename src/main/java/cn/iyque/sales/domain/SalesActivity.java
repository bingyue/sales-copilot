package cn.iyque.sales.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name="iyque_sales_activity")
public class SalesActivity {
    @Id private String id;
    @Column(name="lead_id", nullable=false) private String leadId;
    @Column(nullable=false, length=32) private String type;
    @Column(length=24) private String role;
    @Column(nullable=false, length=12000) private String content;
    @Column(name="source_ref", unique=true, length=240) private String sourceRef;
    @Column(length=120) private String actor;
    @Column(name="occurred_at", nullable=false) private Instant occurredAt;
    @Column(name="created_at", nullable=false) private Instant createdAt;
}
