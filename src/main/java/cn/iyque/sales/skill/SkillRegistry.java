package cn.iyque.sales.skill;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SkillRegistry {
    public record Skill(String id,String version,String title,String prompt,com.fasterxml.jackson.databind.JsonNode inputSchema,com.fasterxml.jackson.databind.JsonNode outputSchema) {}
    private final Map<String,Skill> skills = new LinkedHashMap<>();
    public SkillRegistry() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(String id:List.of("analyze-lead","suggest-reply","plan-followup")) {
            String base="sales-skills/"+id+"/";
            Map<String,Object> manifest;
            try(var in=new ClassPathResource(base+"manifest.yml").getInputStream()) { manifest=new org.yaml.snakeyaml.Yaml().load(in); }
            if(!id.equals(manifest.get("id")) || !Set.of("lead","conversation","pendingTasks","products").containsAll((List<String>)manifest.get("allowedReads"))) throw new IllegalStateException("Invalid Skill manifest");
            for(String file:List.of("prompt","inputSchema","outputSchema")) if(!String.valueOf(manifest.get(file)).matches("[a-zA-Z0-9.-]+")) throw new IllegalStateException("Invalid Skill resource");
            try(var prompt=new ClassPathResource(base+manifest.get("prompt")).getInputStream();
                var input=new ClassPathResource(base+manifest.get("inputSchema")).getInputStream();
                var output=new ClassPathResource(base+manifest.get("outputSchema")).getInputStream()) {
                skills.put(id,new Skill(id,String.valueOf(manifest.get("version")),String.valueOf(manifest.get("title")),new String(prompt.readAllBytes(),StandardCharsets.UTF_8),mapper.readTree(input),mapper.readTree(output)));
            }
        }
    }
    public Skill get(String id) {
        Skill skill=skills.get(id); if(skill==null) throw cn.iyque.sales.service.SalesRules.bad("未知的销售 Skill"); return skill;
    }
    public List<Map<String,String>> list() { return skills.values().stream().map(s->Map.of("id",s.id(),"version",s.version(),"title",s.title())).toList(); }
}
