package cn.iyque.sales.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name="iyque_sales_followup_task")
public class SalesFollowupTask {
    @Id private String id;
    @Column(name="lead_id", nullable=false) private String leadId;
    @Column(nullable=false, length=1000) private String action;
    @Column(length=2000) private String reason;
    @Column(name="due_at", nullable=false) private Instant dueAt;
    @Column(nullable=false, length=24) private String status = "PENDING";
    @Column(name="result_note", length=4000) private String resultNote;
    @Column(name="request_key", nullable=false, unique=true, length=120) private String requestKey;
    @Column(name="run_id") private String runId;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="completed_at") private Instant completedAt;
    @Version private long version;
}
