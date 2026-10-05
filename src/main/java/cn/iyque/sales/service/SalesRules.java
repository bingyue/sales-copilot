package cn.iyque.sales.service;

import cn.iyque.sales.domain.SalesLead;
import java.time.Instant;
import java.util.Set;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public final class SalesRules {
    public static final Set<String> STAGES = Set.of("NEW", "CONTACTED", "QUALIFIED", "OFFERED", "WON", "LOST");
    private SalesRules() {}
    public static String text(String value, int max, String label, boolean required) {
        String v = value == null ? "" : value.trim();
        if ((required && v.isEmpty()) || v.length() > max) throw bad(label + "不能为空或超过长度限制");
        return v;
    }
    public static String key(String key) {
        String k = text(key, 100, "请求标识", true);
        if (!k.matches("[A-Za-z0-9_-]{8,100}")) throw bad("请求标识格式错误");
        return k;
    }
    public static void canFollow(SalesLead lead) {
        if (lead.isStopFollowup() || Set.of("WON", "LOST").contains(lead.getStage())) throw bad("该客户已停止转化跟进");
    }
    public static void due(Instant due) {
        if (due == null || due.isBefore(Instant.now().minusSeconds(60)) || due.isAfter(Instant.now().plusSeconds(366L*86400)))
            throw bad("跟进时间需在现在至一年内");
    }
    public static void version(long actual, long expected) {
        if (actual != expected) throw new ResponseStatusException(HttpStatus.CONFLICT, "资料已变化，请刷新后操作");
    }
    public static ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }
    public static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"); }
}
