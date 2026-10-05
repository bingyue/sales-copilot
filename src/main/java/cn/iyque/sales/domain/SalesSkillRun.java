package cn.iyque.sales.domain;

import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name="iyque_sales_skill_run")
public class SalesSkillRun {
    @Id private String id;
    @Column(name="lead_id", nullable=false) private String leadId;
    @Column(name="skill_id", nullable=false, length=80) private String skillId;
    @Column(name="skill_version", nullable=false, length=32) private String skillVersion;
    @Column(name="model_name", length=120) private String modelName;
    @Column(name="context_version", nullable=false) private long contextVersion;
    @Column(name="request_key", nullable=false, unique=true, length=120) private String requestKey;
    @Lob @Column(name="input_json") private String inputJson;
    @Lob @Column(name="output_json") private String outputJson;
    @Column(nullable=false, length=24) private String status;
    @Column(name="error_code", length=120) private String errorCode;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="finished_at") private Instant finishedAt;
    @Column(name="applied_at") private Instant appliedAt;
}
