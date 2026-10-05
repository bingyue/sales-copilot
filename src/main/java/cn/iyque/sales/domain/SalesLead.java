package cn.iyque.sales.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name="iyque_sales_lead")
public class SalesLead {
    @Id private String id;
    @Column(name="customer_key", unique=true, length=240) private String customerKey;
    @Column(name="external_user_id") private String externalUserId;
    @Column(name="owner_user_id") private String ownerUserId;
    @Column(nullable=false, length=120) private String name;
    @Column(length=80) private String source;
    @Column(name="product_id", length=80) private String productId;
    @Column(length=4000) private String needs;
    @Column(length=4000) private String concerns;
    @Column(nullable=false, length=24) private String stage = "NEW";
    @Column(name="stop_followup", nullable=false) private boolean stopFollowup;
@Column(name="context_version", nullable=false) private long contextVersion;
@Column(name="archive_sequence", nullable=false) private long archiveSequence;
    @Version private long version;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
}
