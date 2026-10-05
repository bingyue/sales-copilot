package cn.iyque.sales;

import cn.iyque.sales.skill.SkillOutputValidator;
import cn.iyque.sales.service.SalesCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkillOutputValidatorTest {
    SalesCatalog catalog=mock(SalesCatalog.class);
    SkillOutputValidator validator=new SkillOutputValidator(new ObjectMapper().findAndRegisterModules(),catalog);
    Map<String,Object> context=Map.of("profileEvidenceId","profile:one","conversation",List.of(Map.of("id","msg-one","role","CUSTOMER","content","我想了解服务")));
    @Test void rejectsEvidenceFromAnotherCustomer() {
        assertThatThrownBy(()->validator.validate("suggest-reply","{\"reply\":\"您好\",\"materialIds\":[],\"questions\":[],\"evidenceIds\":[\"another-customer\"]}",context)).hasMessageContaining("依据");
    }
    @Test void rejectsInventedPriceOrUrl() {
        for(String reply:List.of("只要799元","请访问https://madeup.example")) {
            assertThatThrownBy(()->validator.validate("suggest-reply","{\"reply\":\""+reply+"\",\"materialIds\":[],\"questions\":[],\"evidenceIds\":[\"msg-one\"]}",context)).isInstanceOf(Exception.class);
        }
    }
    @Test void rejectsUnknownFieldsAndMalformedTypes() {
        assertThatThrownBy(()->validator.validate("suggest-reply","{\"reply\":true,\"materialIds\":[],\"questions\":[],\"evidenceIds\":[\"msg-one\"]}",context)).hasMessageContaining("类型");
        assertThatThrownBy(()->validator.validate("suggest-reply","{\"reply\":\"您好\",\"materialIds\":[],\"questions\":[],\"evidenceIds\":[\"msg-one\"],\"sendNow\":true}",context)).hasMessageContaining("未知字段");
    }
    @Test void validReplyKeepsTraceableEvidence() throws Exception {
        when(catalog.all()).thenReturn(List.of());
        var output=validator.validate("suggest-reply","{\"reply\":\"您目前最想解决哪个问题？\",\"materialIds\":[],\"questions\":[],\"evidenceIds\":[\"msg-one\"]}",context);
        assertThat(output.path("evidenceIds").get(0).asText()).isEqualTo("msg-one");
    }
    @Test void modelCannotDeclareReceiptOrWonStage() {
        assertThatThrownBy(()->validator.validate("analyze-lead","{\"needs\":\"购买\",\"concerns\":\"\",\"productId\":\"\",\"stageSuggestion\":\"WON\",\"missingInfo\":[],\"evidenceIds\":[\"msg-one\"]}",context)).hasMessageContaining("阶段");
    }
}
