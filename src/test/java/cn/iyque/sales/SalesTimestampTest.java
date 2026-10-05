package cn.iyque.sales;

import cn.iyque.config.JacksonConfig;
import cn.iyque.sales.domain.SalesLead;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class SalesTimestampTest {
    @Test void responseUsesIsoTimestampInsteadOfEpochSeconds() throws Exception {
        var mapper=new JacksonConfig().jackson2HttpMessageConverter().getObjectMapper();
        var lead=new SalesLead(); lead.setCreatedAt(Instant.parse("2026-10-05T12:00:00Z"));
        var json=mapper.readTree(mapper.writeValueAsString(lead));
        assertThat(json.path("createdAt").isTextual()).isTrue();
        assertThat(json.path("createdAt").asText()).isEqualTo("2026-10-05T12:00:00Z");
    }
}
