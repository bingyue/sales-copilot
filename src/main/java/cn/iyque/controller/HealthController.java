package cn.iyque.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {
    private final JdbcTemplate jdbc;
    @GetMapping("/health")
    public ResponseEntity<Map<String,String>> health() {
        try {
            for(String table:new String[]{"lead","activity","followup_task","receipt","skill_run","sync_cursor"})
                jdbc.queryForList("select 1 from iyque_sales_"+table+" limit 1");
            return ResponseEntity.ok(Map.of("status","ready","product","sales-copilot","version","1.0.0"));
        } catch(Exception e) { return ResponseEntity.status(503).body(Map.of("status","unavailable")); }
    }
}
