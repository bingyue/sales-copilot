package cn.iyque.sales.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name="iyque_sales_receipt")
public class SalesReceipt {
    @Id private String id;
    @Column(name="lead_id", nullable=false) private String leadId;
    @Column(name="product_id", length=80) private String productId;
    @Column(nullable=false, precision=14, scale=2) private java.math.BigDecimal amount;
    @Column(nullable=false, length=3) private String currency = "CNY";
    @Column(name="paid_at", nullable=false) private Instant paidAt;
    @Column(nullable=false, length=24) private String status = "CONFIRMED";
    @Column(name="request_key", nullable=false, unique=true, length=120) private String requestKey;
    @Column(name="receipt_ref", unique=true, length=180) private String receiptRef;
    @Column(length=120) private String actor;
    @Column(name="void_reason", length=1000) private String voidReason;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="voided_at") private Instant voidedAt;
}
