package cn.iyque.sales;

import cn.iyque.dao.IYQueCustomerInfoDao;
import cn.iyque.sales.domain.*;
import cn.iyque.sales.repository.*;
import cn.iyque.sales.service.*;
import cn.iyque.sales.dto.SalesRequests.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=SalesServiceTest.Config.class,properties={
    "spring.datasource.url=jdbc:h2:mem:sales;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false","ADMIN_PASSWORD=test-only"})
class SalesServiceTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses=SalesLead.class)
    @EnableJpaRepositories(basePackageClasses=SalesLeadRepository.class)
    @Import({SalesService.class, SalesArchiveImport.class, cn.iyque.sales.skill.SkillRegistry.class, cn.iyque.sales.skill.SkillRunner.class, cn.iyque.sales.skill.SkillOutputValidator.class, cn.iyque.sales.skill.SalesContextService.class})
    static class Config {}
    @Autowired SalesService sales;
    @Autowired SalesLeadRepository leads;
    @Autowired SalesFollowupTaskRepository tasks;
    @Autowired SalesSkillRunRepository runs;
    @MockBean IYQueCustomerInfoDao customers;
    @MockBean SalesCatalog catalog;
    @MockBean cn.iyque.dao.IYqueMsgAuditDao archive;
    @MockBean cn.iyque.factory.AiModelFactory models;
    @MockBean cn.iyque.properties.AiModelsProperties modelProperties;
    @Autowired cn.iyque.sales.skill.SkillRunner runner;
    @Autowired SalesArchiveImport importer;
    @org.junit.jupiter.api.BeforeEach void setupMocks() {
        org.mockito.Mockito.when(catalog.version()).thenReturn("test-catalog-v1");
    }
    String key() { return UUID.randomUUID().toString(); }
    SalesLead lead() { return sales.create(new CreateLead(null,"验收客户","小红书","运营"),"admin"); }
    SalesFollowupTask task(SalesLead lead,String key) { return sales.createTask(new CreateTask(lead.getId(),"确认试用反馈","客户约定",Instant.now().plusSeconds(3600),key),"admin"); }
    @Test void duplicateTaskDoesNotCreateAnotherTask() {
        var l=lead(); String k=key(); var one=task(l,k); var two=task(l,k);
        assertThat(two.getId()).isEqualTo(one.getId()); assertThat(tasks.findByLeadIdOrderByDueAtAsc(l.getId())).hasSize(1);
    }
    @Test void revenueRequiresReceiptAndStopsPendingTasks() {
        var l=lead(); var t=task(l,key()); var current=sales.get(l.getId());
        assertThatThrownBy(()->sales.update(l.getId(),new UpdateLead(current.getVersion(),l.getName(),"小红书","","","","WON",false),"admin")).isInstanceOf(ResponseStatusException.class);
        String k=key(); var input=new CreateReceipt("",new BigDecimal("799.00"),Instant.now(),null,k);
        var one=sales.receipt(l.getId(),input,"admin"); var two=sales.receipt(l.getId(),input,"admin");
        assertThat(one.getId()).isEqualTo(two.getId()); assertThat(sales.get(l.getId()).getStage()).isEqualTo("WON");
        assertThat(tasks.findById(t.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
        assertThatThrownBy(()->task(l,key())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void voidingLastReceiptResetsStageWithoutReopeningTasks() {
        var l=lead(); var t=task(l,key()); var receipt=sales.receipt(l.getId(),new CreateReceipt("",BigDecimal.TEN,Instant.now(),null,key()),"admin");
        sales.voidReceipt(receipt.getId(),new VoidReceipt("重复录入","CONTACTED"),"admin");
        assertThat(sales.get(l.getId()).getStage()).isEqualTo("CONTACTED");
        assertThat(tasks.findById(t.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
    }
    @Test void invalidVoidRollsBackReceiptAndStage() {
        var l=lead(); var r=sales.receipt(l.getId(),new CreateReceipt("",BigDecimal.TEN,Instant.now(),null,key()),"admin");
        assertThatThrownBy(()->sales.voidReceipt(r.getId(),new VoidReceipt("错误","WON"),"admin")).isInstanceOf(ResponseStatusException.class);
        assertThat(sales.get(l.getId()).getStage()).isEqualTo("WON");
        assertThat(((java.util.List<SalesReceipt>)sales.detail(l.getId()).get("receipts")).get(0).getStatus()).isEqualTo("CONFIRMED");
    }
    @Test void staleProfileCannotOverwriteNewConversation() {
        var l=lead(); sales.addActivity(l.getId(),new AddActivity("CUSTOMER","暂时不需要",Instant.now(),key()),"admin");
        assertThatThrownBy(()->sales.update(l.getId(),new UpdateLead(l.getVersion(),l.getName(),"","","","","CONTACTED",false),"admin")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void explicitStopCancelsTasksAndRejectsNewOnes() {
        var l=lead(); var t=task(l,key()); l=sales.get(l.getId());
        var stopped=sales.update(l.getId(),new UpdateLead(l.getVersion(),l.getName(),"","","","","CONTACTED",true),"admin");
        assertThat(tasks.findById(t.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
        assertThatThrownBy(()->task(stopped,key())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void completionRequiresActualResult() {
        var l=lead(); var t=task(l,key());
        assertThatThrownBy(()->sales.actOnTask(t.getId(),"complete",new TaskAction(t.getVersion(),"",null),"admin")).isInstanceOf(ResponseStatusException.class);
        var done=sales.actOnTask(t.getId(),"complete",new TaskAction(t.getVersion(),"已沟通，等待确认",null),"admin");
        assertThat(done.getStatus()).isEqualTo("DONE");
        assertThat(sales.actOnTask(t.getId(),"complete",new TaskAction(t.getVersion(),"重复点击",null),"admin").getResultNote()).isEqualTo("已沟通，等待确认");
    }
    @Test void staleAiProposalIsRejected() {
        var l=lead(); var run=new SalesSkillRun(); run.setId(key()); run.setLeadId(l.getId()); run.setSkillId("analyze-lead"); run.setSkillVersion("1"); run.setRequestKey(key());
        run.setContextVersion(l.getContextVersion()); run.setStatus("SUCCEEDED"); run.setCreatedAt(Instant.now()); run.setOutputJson("{\"needs\":\"旧需求\",\"concerns\":\"\",\"productId\":\"\"}"); runs.saveAndFlush(run);
        sales.addActivity(l.getId(),new AddActivity("CUSTOMER","需求变了",Instant.now(),key()),"admin");
        assertThatThrownBy(()->sales.apply(run.getId(),new ApplySkill(run.getContextVersion()),"admin")).isInstanceOf(ResponseStatusException.class);
        assertThat(sales.get(l.getId()).getNeeds()).isNull();
    }
    @Test void receiptRejectsInvalidAmountAndDuplicateReference() {
        var l=lead(); assertThatThrownBy(()->sales.receipt(l.getId(),new CreateReceipt("",new BigDecimal("0.001"),Instant.now(),null,key()),"admin")).isInstanceOf(ResponseStatusException.class);
        String ref=key(); sales.receipt(l.getId(),new CreateReceipt("",BigDecimal.TEN,Instant.now(),ref,key()),"admin");
        var other=lead(); assertThatThrownBy(()->sales.receipt(other.getId(),new CreateReceipt("",BigDecimal.TEN,Instant.now(),ref,key()),"admin")).isInstanceOf(ResponseStatusException.class);
    }

    @Test void threeSkillsRunValidateAndApplyWithoutSendingMessages() throws Exception {
        var l=lead(); var chat=org.mockito.Mockito.mock(dev.langchain4j.model.chat.ChatLanguageModel.class);
        var config=new cn.iyque.properties.AiModelsProperties.ModelConfig(); config.setApiKey("test-key"); config.setBaseUrl("https://example.invalid"); config.setModelName("fixture");
        org.mockito.Mockito.when(modelProperties.getEnabled()).thenReturn(java.util.List.of("test"));
        org.mockito.Mockito.when(modelProperties.getConfigs()).thenReturn(java.util.Map.of("test",config));
        org.mockito.Mockito.when(models.getChatModel("test",0.2,0.8)).thenReturn(chat);
        String evidence="\"evidenceIds\":[\"profile:"+l.getId()+"\"]";
        String[] skills={"analyze-lead","suggest-reply","plan-followup"};
        String[] outputs={"{\"needs\":\"需要整理跟进\",\"concerns\":\"\",\"productId\":\"\",\"stageSuggestion\":\"CONTACTED\",\"missingInfo\":[],"+evidence+"}",
                "{\"reply\":\"可以先介绍一下目前的跟进流程吗？\",\"materialIds\":[],\"questions\":[],"+evidence+"}",
                "{\"action\":\"确认现有流程\",\"reason\":\"需求待确认\",\"dueAt\":\""+Instant.now().plusSeconds(86400)+"\","+evidence+"}"};
        for(int i=0;i<skills.length;i++) {
            var response=dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(dev.langchain4j.data.message.AiMessage.from(outputs[i])).build();
            org.mockito.Mockito.when(chat.chat(org.mockito.ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenReturn(response);
            String request=key(); var run=runner.run(l.getId(),new RunSkill(skills[i],request));
            assertThat(run.getStatus()).isEqualTo("SUCCEEDED");
            assertThat(runner.run(l.getId(),new RunSkill(skills[i],request)).getId()).isEqualTo(run.getId());
            sales.apply(run.getId(),new ApplySkill(run.getContextVersion()),"admin");
            assertThat(sales.apply(run.getId(),new ApplySkill(run.getContextVersion()),"admin").getAppliedAt()).isNotNull();
        }
        assertThat(tasks.findByLeadIdAndStatus(l.getId(),"PENDING")).hasSize(1);
        assertThat(sales.get(l.getId()).getStage()).isEqualTo("NEW");
        assertThat(sales.get(l.getId()).getNeeds()).isEqualTo("需要整理跟进");
        org.mockito.Mockito.verify(chat,org.mockito.Mockito.times(3)).chat(org.mockito.ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList());
    }
    @Test void archivedMessagesUseExactCustomerEmployeePairAndInvalidateContext() {
        var l=lead(); l.setCustomerKey("customer&employee"); l.setExternalUserId("customer"); l.setOwnerUserId("employee"); leads.saveAndFlush(l);
        var m=cn.iyque.entity.IYqueMsgAudit.builder().msgId("archive-message").fromId("customer").acceptId("employee").content("需要演示").dataSeq(23L).msgTime(new java.util.Date()).build();
        org.mockito.Mockito.when(archive.findSalesConversation(org.mockito.ArgumentMatchers.eq("customer"),org.mockito.ArgumentMatchers.eq("employee"),org.mockito.ArgumentMatchers.eq(0L),org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(m));
        long before=sales.get(l.getId()).getContextVersion(); assertThat(importer.importStored(l.getId())).isEqualTo(1);
        assertThat(sales.get(l.getId()).getArchiveSequence()).isEqualTo(23); assertThat(sales.get(l.getId()).getContextVersion()).isGreaterThan(before);
        assertThat(importer.importStored(l.getId())).isZero();
    }
}
