package cn.iyque.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

public final class SalesRequests {
    private SalesRequests() {}
    public record CreateLead(String customerKey, String name, String source, String ownerUserId) {}
    public record UpdateLead(long version, String name, String source, String productId,
                             String needs, String concerns, String stage, boolean stopFollowup) {}
    public record AddActivity(String role, String content, Instant occurredAt, String requestKey) {}
    public record CreateTask(String leadId, String action, String reason, Instant dueAt, String requestKey) {}
    public record TaskAction(long version, String resultNote, Instant dueAt) {}
    public record CreateReceipt(String productId, BigDecimal amount, Instant paidAt, String receiptRef, String requestKey) {}
    public record VoidReceipt(String reason, String stage) {}
    public record RunSkill(String skillId, String requestKey) {}
    public record ApplySkill(long contextVersion) {}
}
