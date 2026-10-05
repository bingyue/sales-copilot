<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, stages, date, money } from './api'
const router = useRouter()
const metrics = ref<any>({})
const leads = ref<any[]>([]), tasks = ref<any[]>([])
const total = ref(0), taskTotal = ref(0), page = ref(1), taskPage = ref(1)
const query = ref(''), stage = ref(''), scope = ref('today'), taskStatus = ref('PENDING')
const tab = ref('tasks'), loading = ref(false), failed = ref(false), creating = ref(false), showCreate = ref(false)
const form = reactive({ name: '', source: '小红书', ownerUserId: '' })
async function loadLeads() {
  const result = await api.leads({ query: query.value, stage: stage.value, page: page.value - 1, size: 20 })
  leads.value = result.content; total.value = Number(result.totalElements)
}
async function loadTasks() {
  const result = await api.tasks({ scope: scope.value, status: taskStatus.value, page: taskPage.value - 1, size: 20 })
  tasks.value = result.content; taskTotal.value = Number(result.totalElements)
}
async function load() {
  loading.value = true; failed.value = false
  try { await Promise.all([api.metrics().then(r => metrics.value = r), loadLeads(), loadTasks()]) }
  catch { failed.value = true } finally { loading.value = false }
}
async function searchLeads() { page.value = 1; try { await loadLeads() } catch { failed.value = true } }
async function searchTasks() { taskPage.value = 1; try { await loadTasks() } catch { failed.value = true } }
async function create() {
  if (!form.name.trim()) return ElMessage.warning('请填写客户名称')
  creating.value = true
  try { const lead = await api.create(form); showCreate.value = false; router.push(`/sales/leads/${lead.id}`) }
  catch {} finally { creating.value = false }
}
onMounted(load)
</script>

<template>
  <main class="sales-workspace" v-loading="loading">
    <header class="workspace-hero">
      <div><div class="eyebrow">JIZHI SALES COPILOT</div><h1>每个客户，都有下一步。</h1><p>整理需求，持续跟进，让意向成为可记录的成交。</p></div>
      <div class="hero-actions"><el-button @click="router.push('/manage/customer')">企微客户</el-button><el-button type="primary" @click="showCreate = true">＋ 新建销售档案</el-button></div>
    </header>
    <el-alert v-if="failed" title="部分数据加载失败，请重试" type="error" :closable="false" show-icon><el-button link @click="load">重新加载</el-button></el-alert>
    <section class="metrics">
      <article><span>今日待跟进 · 含逾期</span><strong>{{ metrics.dueToday ?? '—' }}</strong><small :class="{danger: Number(metrics.overdue) > 0}">{{ metrics.overdue ?? 0 }} 项已逾期</small></article>
      <article><span>本月新增档案</span><strong>{{ metrics.newLeadsThisMonth ?? '—' }}</strong><small>累计 {{ metrics.totalLeads ?? 0 }} 份销售档案</small></article>
      <article><span>累计已成交档案</span><strong>{{ metrics.wonLeads ?? '—' }}</strong><small>以有效实收登记为准</small></article>
      <article><span>本月有效实收</span><strong><i>¥</i>{{ money(metrics.revenueThisMonth) }}</strong><small>按收款日期统计，不含作废</small></article>
    </section>
    <section class="work-panel">
      <el-tabs v-model="tab">
        <el-tab-pane label="跟进工作台" name="tasks">
          <div class="filters"><el-radio-group v-model="scope" @change="searchTasks"><el-radio-button value="today">今天及之前</el-radio-button><el-radio-button value="overdue">已逾期</el-radio-button><el-radio-button value="all">全部时间</el-radio-button></el-radio-group>
            <el-select v-model="taskStatus" @change="searchTasks" style="width:140px"><el-option label="待完成" value="PENDING"/><el-option label="已完成" value="DONE"/><el-option label="已取消" value="CANCELLED"/></el-select>
          </div>
          <div v-if="!tasks.length" class="empty"><div class="empty-symbol">✓</div><h3>当前没有{{ taskStatus === 'PENDING' ? '待办' : '匹配的' }}任务</h3><p>打开客户档案，记录沟通并安排下一次跟进。</p><el-button @click="tab = 'leads'">查看销售档案</el-button></div>
          <div class="task-row" v-for="row in tasks" :key="row.task.id">
            <div class="avatar">{{ row.leadName.slice(0, 1) }}</div>
            <div class="task-main"><h3>{{ row.leadName }}<span>{{ row.task.status === 'DONE' ? '已完成' : row.task.status === 'CANCELLED' ? '已取消' : '待跟进' }}</span></h3><p>{{ row.task.action }}</p><small>{{ row.task.reason || '手动安排的跟进' }}</small></div>
            <div class="task-time" :class="{danger: row.task.status === 'PENDING' && new Date(row.task.dueAt).getTime() < Date.now()}">{{ date(row.task.dueAt) }}</div>
            <el-button @click="router.push(`/sales/leads/${row.task.leadId}`)">处理跟进 →</el-button>
          </div>
          <el-pagination v-if="taskTotal > 20" v-model:current-page="taskPage" :total="taskTotal" :page-size="20" layout="prev, pager, next" @current-change="loadTasks"/>
        </el-tab-pane>
        <el-tab-pane label="销售档案" name="leads">
          <div class="filters"><el-input v-model="query" placeholder="搜索客户名称" clearable style="max-width:280px" @keyup.enter="searchLeads" @clear="searchLeads"/><el-select v-model="stage" placeholder="所有销售阶段" clearable style="width:170px" @change="searchLeads"><el-option v-for="(label, value) in stages" :key="value" :label="label" :value="value"/></el-select><el-button @click="searchLeads">查询</el-button></div>
          <el-table :data="leads" empty-text="还没有销售档案，点击右上角新建，或从企微客户导入" @row-dblclick="(row: any) => router.push(`/sales/leads/${row.id}`)">
            <el-table-column label="客户" prop="name" min-width="150"/><el-table-column label="来源" prop="source" min-width="110"/>
            <el-table-column label="销售阶段" width="140"><template #default="{row}"><el-tag :type="row.stage === 'WON' ? 'success' : row.stage === 'LOST' ? 'info' : 'primary'">{{ stages[row.stage] }}</el-tag></template></el-table-column>
            <el-table-column label="需求摘要" prop="needs" min-width="220" show-overflow-tooltip/><el-table-column label="最近更新" min-width="185"><template #default="{row}">{{ date(row.updatedAt) }}</template></el-table-column>
            <el-table-column width="110"><template #default="{row}"><el-button link type="primary" @click="router.push(`/sales/leads/${row.id}`)">打开档案</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-if="total > 20" v-model:current-page="page" :total="total" :page-size="20" layout="prev, pager, next" @current-change="loadLeads"/>
        </el-tab-pane>
      </el-tabs>
    </section>
    <el-dialog v-model="showCreate" title="新建销售档案" width="460px">
      <el-form label-position="top"><el-form-item label="客户名称" required><el-input v-model="form.name" maxlength="120" placeholder="填写便于识别的客户称呼"/></el-form-item><el-form-item label="获客来源"><el-select v-model="form.source" filterable allow-create><el-option v-for="s in ['小红书','抖音','企业微信','转介绍','其他']" :key="s" :value="s" :label="s"/></el-select></el-form-item><el-form-item label="负责人"><el-input v-model="form.ownerUserId" maxlength="120" placeholder="可选：员工姓名或标识"/></el-form-item></el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" :loading="creating" @click="create">建立档案</el-button></template>
    </el-dialog>
  </main>
</template>

<style scoped>
.sales-workspace{padding:26px;max-width:1600px;margin:auto;color:#172f3b}.workspace-hero{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:28px}.eyebrow{font-size:11px;letter-spacing:2px;color:#168579;font-weight:700}h1{font-size:28px;letter-spacing:-.5px;margin:10px 0}.workspace-hero p{color:#71828c;margin:0;font-size:14px}.hero-actions{display:flex;gap:4px}.metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px;margin:24px 0}.metrics article{padding:22px;background:var(--BgWhite,#fff);border:1px solid #e5ebee;border-radius:14px;display:flex;flex-direction:column;gap:12px}.metrics span{font-size:13px;color:#657b86}.metrics strong{font-size:32px;font-weight:650;letter-spacing:-1px}.metrics i{font-size:20px;font-style:normal;margin-right:5px}.metrics small{font-size:12px;color:#8a969e}.danger{color:#c75b43!important}.work-panel{background:var(--BgWhite,#fff);border:1px solid #e5ebee;border-radius:14px;padding:12px 24px 24px}.filters{display:flex;gap:12px;align-items:center;margin:12px 0 24px}.task-row{display:flex;align-items:center;gap:18px;padding:23px 0;border-bottom:1px solid #eef1f4}.avatar{background:#e5f3ef;color:#168579;border-radius:13px;padding:13px;font-size:20px;font-weight:600}.task-main{flex:1;min-width:160px}.task-main h3{font-size:15px;margin:0 0 8px}.task-main h3 span{font-size:11px;font-weight:400;background:#f1f5f6;color:#74868f;margin-left:10px;padding:3px 7px;border-radius:5px}.task-main p{font-size:14px;margin:0 0 6px}.task-main small,.task-time{font-size:12px;color:#81929b}.empty{text-align:center;padding:50px 20px}.empty-symbol{width:52px;height:52px;line-height:52px;background:#eef7f3;color:#248875;border-radius:50%;margin:0 auto;font-size:25px}.empty h3{font-size:17px;margin-top:18px}.empty p{color:#80909a;font-size:13px;margin-bottom:22px}.el-pagination{margin-top:24px;justify-content:flex-end}@media(max-width:1000px){.metrics{grid-template-columns:repeat(2,1fr)}.workspace-hero{align-items:flex-start;flex-direction:column}.task-row{flex-wrap:wrap}.sales-workspace{padding:16px}}
</style>
