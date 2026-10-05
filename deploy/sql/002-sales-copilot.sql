-- Additive sales schema. Existing Iyque customer state/status meanings are preserved.
CREATE TABLE IF NOT EXISTS iyque_sales_lead (
 id VARCHAR(36) PRIMARY KEY, customer_key VARCHAR(240) UNIQUE,
 external_user_id VARCHAR(255), owner_user_id VARCHAR(255), name VARCHAR(120) NOT NULL,
 source VARCHAR(80), product_id VARCHAR(80), needs VARCHAR(4000), concerns VARCHAR(4000),
 stage VARCHAR(24) NOT NULL, stop_followup BOOLEAN NOT NULL DEFAULT FALSE,
 context_version BIGINT NOT NULL DEFAULT 0, archive_sequence BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 INDEX idx_sales_stage (stage, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS iyque_sales_sync_cursor (
 id VARCHAR(255) PRIMARY KEY, sequence BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
 last_attempt DATETIME(6), last_error VARCHAR(120)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS iyque_sales_activity (
 id VARCHAR(36) PRIMARY KEY, lead_id VARCHAR(36) NOT NULL, type VARCHAR(32) NOT NULL,
 role VARCHAR(24), content TEXT NOT NULL, source_ref VARCHAR(240) UNIQUE, actor VARCHAR(120),
 occurred_at DATETIME(6) NOT NULL, created_at DATETIME(6) NOT NULL,
 INDEX idx_sales_activity (lead_id, occurred_at),
 FOREIGN KEY (lead_id) REFERENCES iyque_sales_lead(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS iyque_sales_followup_task (
 id VARCHAR(36) PRIMARY KEY, lead_id VARCHAR(36) NOT NULL,
 action VARCHAR(1000) NOT NULL, reason VARCHAR(2000), due_at DATETIME(6) NOT NULL,
 status VARCHAR(24) NOT NULL, result_note VARCHAR(4000), request_key VARCHAR(120) NOT NULL UNIQUE,
 run_id VARCHAR(36), created_at DATETIME(6) NOT NULL, completed_at DATETIME(6), version BIGINT NOT NULL DEFAULT 0,
 INDEX idx_sales_task_due (status,due_at), INDEX idx_sales_task_lead (lead_id),
 FOREIGN KEY (lead_id) REFERENCES iyque_sales_lead(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS iyque_sales_skill_run (
 id VARCHAR(36) PRIMARY KEY, lead_id VARCHAR(36) NOT NULL, skill_id VARCHAR(80) NOT NULL,
 skill_version VARCHAR(32) NOT NULL, model_name VARCHAR(120), context_version BIGINT NOT NULL,
 request_key VARCHAR(120) NOT NULL UNIQUE, input_json LONGTEXT, output_json LONGTEXT,
 status VARCHAR(24) NOT NULL, error_code VARCHAR(120), created_at DATETIME(6) NOT NULL,
 finished_at DATETIME(6), applied_at DATETIME(6), INDEX idx_sales_run (lead_id,created_at),
 FOREIGN KEY (lead_id) REFERENCES iyque_sales_lead(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS iyque_sales_receipt (
 id VARCHAR(36) PRIMARY KEY, lead_id VARCHAR(36) NOT NULL, product_id VARCHAR(80),
 amount DECIMAL(14,2) NOT NULL, currency VARCHAR(3) NOT NULL DEFAULT 'CNY', paid_at DATETIME(6) NOT NULL,
 status VARCHAR(24) NOT NULL, request_key VARCHAR(120) NOT NULL UNIQUE, receipt_ref VARCHAR(180) UNIQUE,
 actor VARCHAR(120), void_reason VARCHAR(1000), created_at DATETIME(6) NOT NULL, voided_at DATETIME(6),
 INDEX idx_sales_receipt (lead_id,status), INDEX idx_sales_receipt_date (status,paid_at),
 FOREIGN KEY (lead_id) REFERENCES iyque_sales_lead(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
