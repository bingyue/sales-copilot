package cn.iyque.sales.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

@Component
public class SalesCatalog {
    public record Product(String id, String name, String description, BigDecimal price, String priceNote, List<Material> materials) {}
    public record Material(String id, String title, String url, String content) {}
    private final List<Product> products;
    private final String version;
    public SalesCatalog(ObjectMapper mapper, @Value("${sales.catalog:classpath:sales-catalog/products.json}") Resource resource) throws Exception {
        try (var stream = resource.getInputStream()) { products = List.copyOf(mapper.readValue(stream, new TypeReference<List<Product>>() {})); }
        version = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(products)));
        Set<String> ids = new HashSet<>();
        Set<String> materials = new HashSet<>();
        for (Product p : products) {
            if (p.id() == null || !ids.add(p.id()) || p.name() == null) throw new IllegalStateException("产品目录 ID 或名称无效");
            for (Material m : p.materials() == null ? List.<Material>of() : p.materials()) {
                if (m.id() == null || m.id().isBlank() || !materials.add(m.id()) || m.title() == null) throw new IllegalStateException("资料 ID 或名称无效");
                if (m.url() != null && !m.url().isBlank() && !m.url().matches("https?://[^\\s]+")) throw new IllegalStateException("产品资料链接无效");
            }
        }
    }
    public String version() { return version; }
    public List<Product> all() { return products; }
    public void validateId(String id) {
        if (id != null && !id.isBlank() && products.stream().noneMatch(p -> p.id().equals(id))) throw SalesRules.bad("产品不在有效目录中");
    }
}
