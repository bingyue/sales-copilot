package cn.iyque.sales.skill;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.Set;
import static cn.iyque.sales.service.SalesRules.bad;

/** Validates the JSON Schema keywords used by the bundled contracts; not a general schema engine. */
public final class SkillSchemaValidator {
    private SkillSchemaValidator() {}
    public static void validate(JsonNode value,JsonNode schema) {
        String type=schema.path("type").asText();
        boolean valid=switch(type) {
            case "object" -> value.isObject(); case "array" -> value.isArray();
            case "string" -> value.isTextual(); case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber(); case "boolean" -> value.isBoolean();
            default -> throw new IllegalStateException("Unsupported Skill schema type: "+type);
        };
        if(!valid) throw bad("Skill 数据类型与契约不符");
        if(schema.has("enum")) { boolean match=false; for(var option:schema.get("enum")) if(option.equals(value)) match=true; if(!match) throw bad("Skill 枚举值无效"); }
        if(value.isObject()) {
            for(var field:schema.path("required")) if(!value.has(field.asText())) throw bad("Skill 缺少字段："+field.asText());
            var fields=value.fields(); while(fields.hasNext()) { var entry=fields.next(); var property=schema.path("properties").get(entry.getKey());
                if(property!=null) validate(entry.getValue(),property);
                else if(schema.has("additionalProperties")&&!schema.path("additionalProperties").asBoolean()) throw bad("Skill 包含未定义字段");
            }
        } else if(value.isArray()) {
            if(value.size()<schema.path("minItems").asInt(0)||value.size()>schema.path("maxItems").asInt(Integer.MAX_VALUE)) throw bad("Skill 数组长度无效");
            if(schema.has("items")) for(var item:value) validate(item,schema.get("items"));
        } else if(value.isTextual()) {
            int length=value.asText().codePointCount(0,value.asText().length());
            if(length<schema.path("minLength").asInt(0)||length>schema.path("maxLength").asInt(Integer.MAX_VALUE)) throw bad("Skill 文本长度无效");
            if("date-time".equals(schema.path("format").asText())) try { OffsetDateTime.parse(value.asText()); } catch(Exception e) { throw bad("Skill 时间格式无效"); }
        }
    }
}
