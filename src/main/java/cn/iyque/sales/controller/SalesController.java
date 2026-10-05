package cn.iyque.sales.controller;

import cn.iyque.config.IYqueParamConfig;
import cn.iyque.domain.ResponseResult;
import cn.iyque.sales.dto.SalesRequests.*;
import cn.iyque.sales.service.*;
import cn.iyque.sales.skill.*;
import cn.iyque.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {
    private final SalesService sales;
    private final SalesArchiveImport archive;
    private final SkillRunner runner;
    private final SkillRegistry registry;
    private final SalesCatalog catalog;
    private final IYqueParamConfig config;

    @ModelAttribute
    public void requireAdministrator() {
        if(!config.getUserName().equals(SecurityUtils.getCurrentUserName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅内部管理员可访问销售工作台");
    }
    private String actor() { return SecurityUtils.getCurrentUserName(); }
    private ResponseResult<?> ok(Object value) { return new ResponseResult<>(value); }
    @GetMapping("/leads") public ResponseResult<?> list(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="") String stage,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(sales.list(query,stage,page,size)); }
    @PostMapping("/leads") public ResponseResult<?> create(@RequestBody CreateLead input) { return ok(sales.create(input,actor())); }
    @GetMapping("/leads/{id}") public ResponseResult<?> detail(@PathVariable String id) { archive.importStored(id); return ok(sales.detail(id)); }
    @PatchMapping("/leads/{id}") public ResponseResult<?> update(@PathVariable String id,@RequestBody UpdateLead input) { return ok(sales.update(id,input,actor())); }
    @PostMapping("/leads/{id}/activities") public ResponseResult<?> activity(@PathVariable String id,@RequestBody AddActivity input) { return ok(sales.addActivity(id,input,actor())); }
    @GetMapping("/followups") public ResponseResult<?> tasks(@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="today") String scope,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="30") int size) { return ok(sales.listTasks(status,scope,page,size)); }
    @PostMapping("/followups") public ResponseResult<?> task(@RequestBody CreateTask input) { return ok(sales.createTask(input,actor())); }
    @PostMapping("/followups/{id}/{action}") public ResponseResult<?> taskAction(@PathVariable String id,@PathVariable String action,@RequestBody TaskAction input) { return ok(sales.actOnTask(id,action,input,actor())); }
    @PostMapping("/leads/{id}/receipts") public ResponseResult<?> receipt(@PathVariable String id,@RequestBody CreateReceipt input) { return ok(sales.receipt(id,input,actor())); }
    @PostMapping("/receipts/{id}/void") public ResponseResult<?> voidReceipt(@PathVariable String id,@RequestBody VoidReceipt input) { return ok(sales.voidReceipt(id,input,actor())); }
    @PostMapping("/leads/{id}/skill-runs") public ResponseResult<?> run(@PathVariable String id,@RequestBody RunSkill input) { archive.importStored(id); return ok(runner.run(id,input)); }
    @GetMapping("/skill-runs/{id}") public ResponseResult<?> run(@PathVariable String id) { return ok(runner.get(id)); }
    @PostMapping("/skill-runs/{id}/apply") public ResponseResult<?> apply(@PathVariable String id,@RequestBody ApplySkill input) throws Exception { archive.importStored(runner.get(id).getLeadId()); return ok(sales.apply(id,input,actor())); }
    @GetMapping("/metrics") public ResponseResult<?> metrics() { return ok(sales.metrics()); }
    @GetMapping("/catalog") public ResponseResult<?> catalog() { return ok(Map.of("products",catalog.all(),"skills",registry.list())); }
}
