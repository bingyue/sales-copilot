<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, stages, roles, taskStatuses, skillTitles, key, date, money, skillError } from './api'
const route = useRoute(), router = useRouter()
const id = String(route.params.id)
const data = ref<any>(null), catalog = ref<any>({ products: [], skills: [] }), profile = reactive<any>({})
const loading = ref(false), busy = ref(false), generating = ref(''), active = ref('conversation')
const conversation = reactive({ role: 'CUSTOMER', content: '', occurredAt: new Date() })
const showTask = ref(false), showReceipt = ref(false), showPostpone = ref(false), selectedTask = ref<any>(null)
const taskForm = reactive({ action: '', reason: '', dueAt: new Date(Date.now() + 86400000) })
const receiptForm = reactive({ productId: '', amount: undefined as number | undefined, paidAt: new Date(), receiptRef: '' })
const postponeAt = ref(new Date(Date.now() + 86400000))
const lead = computed(() => data.value?.lead || {})
const canFollow = computed(() => !lead.value.stopFollowup && !['WON','LOST'].includes(lead.value.stage))
const product = computed(() => catalog.value.products.find((p: any) => p.id === lead.value.productId))
const aiRuns = computed(() => (data.value?.runs || []).map((r: any) => {
  let output: any = {}, context: any = {}
  try { output = JSON.parse(r.outputJson || '{}'); context = JSON.parse(r.inputJson || '{}') } catch {}
  return { ...r, output, context, stale: Number(r.contextVersion) !== Number(lead.value.contextVersion) }
}))
async function load() {
  loading.value = true
  try { const [detail, config] = await Promise.all([api.detail(id), api.catalog()]); data.value = detail; catalog.value = config; Object.assign(profile, detail.lead) }
  catch {} finally { loading.value = false }
}
async function operate(fn: () => Promise<unknown>, message: string) {
  if (busy.value) return
  busy.value = true
  try { await fn(); ElMessage.success(message); await load(); return true } catch (error) { console.error('Sales action did not complete', error); return false } finally { busy.value = false }
}
async function saveProfile() { await operate(() => api.update(id, profile), '客户档案已更新') }
async function addConversation() {
  if (!conversation.content.trim()) return ElMessage.warning('请填写沟通内容')
  if (await operate(() => api.activity(id, { ...conversation, occurredAt: new Date(conversation.occurredAt).toISOString(), requestKey: key() }), '沟通已记录')) conversation.content = ''
}
async function generate(skillId: string) {
  if (generating.value) return
  generating.value = skillId; active.value = 'ai'
  try { const run = await api.run(id, { skillId, requestKey: key() }); if (run.status === 'FAILED') ElMessage.warning(skillError(run.errorCode)); await load() }
  catch {} finally { generating.value = '' }
}
async function apply(run: any) { await operate(() => api.apply(run.id, Number(run.contextVersion)), '建议已采纳') }
async function copy(value: string) { try { await navigator.clipboard.writeText(value); ElMessage.success('已复制，请在聊天窗口确认后发送') } catch { ElMessage.info('请选中回复内容复制；当前浏览器不允许自动访问剪贴板') } }
async function createTask() {
  if (await operate(() => api.createTask({ ...taskForm, leadId: id, dueAt: new Date(taskForm.dueAt).toISOString(), requestKey: key() }), '跟进已安排')) showTask.value = false
}
async function complete(task: any) {
  try { const { value } = await ElMessageBox.prompt('记录实际沟通结果，完成后会保留在客户时间线中。', '完成跟进', { confirmButtonText: '确认完成', cancelButtonText: '取消', inputType: 'textarea', inputValidator: (v: string) => !!v?.trim() || '请填写跟进结果' }); await operate(() => api.taskAction(task.id, 'complete', { version: Number(task.version), resultNote: value }), '跟进已完成') } catch {}
}
async function cancel(task: any) { try { await ElMessageBox.confirm('取消这项尚未执行的跟进任务？', '取消跟进', { confirmButtonText: '确认取消', cancelButtonText: '返回' }); await operate(() => api.taskAction(task.id, 'cancel', { version: Number(task.version) }), '任务已取消') } catch {} }
async function postpone() {
  if (await operate(() => api.taskAction(selectedTask.value.id, 'postpone', { version: Number(selectedTask.value.version), dueAt: new Date(postponeAt.value).toISOString() }), '跟进时间已更新')) showPostpone.value = false
}
async function saveReceipt() {
  if (!receiptForm.amount || receiptForm.amount <= 0) return ElMessage.warning('请填写实际收到的金额')
  if (await operate(() => api.receipt(id, { ...receiptForm, paidAt: new Date(receiptForm.paidAt).toISOString(), requestKey: key() }), '实收已登记，客户转为已成交')) showReceipt.value = false
}
async function voidReceipt(receipt: any) {
  try { const { value } = await ElMessageBox.prompt('用于录入纠错，请填写原因。若这是最后一笔有效实收，客户将回到“沟通中”，旧跟进任务不会恢复。', '作废实收记录', { confirmButtonText: '确认作废', cancelButtonText: '取消', inputValidator: (v: string) => !!v?.trim() || '请填写原因' }); await operate(() => api.voidReceipt(receipt.id, { reason: value, stage: 'CONTACTED' }), '实收已作废') } catch {}
}
function evidence(run: any, evidenceId: string) {
  if (evidenceId.startsWith('profile:')) return '生成时的档案：' + JSON.stringify({ needs: run.context.lead?.needs, concerns: run.context.lead?.concerns, stage: stages[run.context.lead?.stage] })
  const item = (run.context.conversation || []).find((a: any) => a.id === evidenceId)
  return item ? `${roles[item.role] || item.role} · ${date(item.time)}\n${item.content}` : '原始依据不可用'
}
function materials(ids: string[]) { return catalog.value.products.flatMap((p: any) => p.materials || []).filter((m: any) => ids.includes(m.id)) }
onMounted(load)
</script>

<template>
  <main class="lead-page" v-loading="loading">
    <el-button link @click="router.push('/sales/followups')">← 返回销售工作台</el-button>
    <el-empty v-if="!data && !loading" description="档案加载失败或不存在"><el-button @click="load">重试</el-button></el-empty>
    <template v-if="data">
      <header class="lead-header"><div class="identity"><div class="avatar">{{ lead.name?.slice(0, 1) }}</div><div><h1>{{ lead.name }}<el-tag :type="lead.stage === 'WON' ? 'success' : 'info'">{{ stages[lead.stage] }}</el-tag></h1><p>{{ lead.source || '来源待补充' }} · {{ lead.customerKey ? '已关联企微客户' : '手工销售档案' }} · 更新于 {{ date(lead.updatedAt) }}</p></div></div><el-button type="primary" @click="showReceipt = true">登记实收</el-button></header>
      <el-alert v-if="!canFollow" type="info" :closable="false" title="该客户已成交或停止推进，转化跟进任务已停止。" show-icon/>
      <div class="lead-layout">
        <aside class="profile-card panel"><div class="panel-heading"><h2>客户档案</h2><span>人工确认信息</span></div>
          <el-form label-position="top" size="default">
            <el-form-item label="客户名称"><el-input v-model="profile.name" maxlength="120"/></el-form-item>
            <el-form-item label="获客来源"><el-input v-model="profile.source" maxlength="80" placeholder="小红书 / 抖音 / 转介绍"/></el-form-item>
            <el-form-item label="销售阶段"><el-select v-model="profile.stage"><el-option v-for="(label,value) in stages" :key="value" :label="label" :value="value" :disabled="value === 'WON' ? lead.stage !== 'WON' : lead.stage === 'WON'"/></el-select></el-form-item>
            <el-form-item label="意向产品"><el-select v-model="profile.productId" clearable placeholder="尚未明确"><el-option v-for="p in catalog.products" :key="p.id" :label="p.name" :value="p.id"/></el-select></el-form-item>
            <div v-if="product" class="product-note"><b>{{ product.name }}</b><p v-if="product.price != null">配置报价 ¥{{ money(product.price) }}</p><small>{{ product.priceNote }}</small></div>
            <el-form-item label="客户需求"><el-input v-model="profile.needs" type="textarea" :rows="3" maxlength="4000" placeholder="已经确认的需求；未知信息留空"/></el-form-item>
            <el-form-item label="主要顾虑"><el-input v-model="profile.concerns" type="textarea" :rows="3" maxlength="4000" placeholder="价格、适配、时间等顾虑"/></el-form-item>
            <el-form-item><el-switch v-model="profile.stopFollowup" active-text="停止转化跟进"/></el-form-item>
            <el-button type="primary" plain :loading="busy" @click="saveProfile" style="width:100%">保存档案</el-button>
          </el-form>
        </aside>
        <section class="main-column">
          <div class="copilot-card"><div><div class="eyebrow">YOUR SALES COPILOT</div><h2>一起想好下一步</h2><p>根据已记录的信息生成建议，由你决定如何推进。</p></div><div class="skill-buttons"><el-button v-for="(label,skill) in skillTitles" :key="skill" :loading="generating === skill" :disabled="!!generating || (skill === 'plan-followup' && !canFollow)" @click="generate(skill)">{{ label }}</el-button></div></div>
          <div class="panel detail-panel"><el-tabs v-model="active">
            <el-tab-pane label="沟通时间线" name="conversation">
              <div class="conversation-compose"><div class="compose-controls"><el-select v-model="conversation.role" style="width:140px"><el-option label="客户发言" value="CUSTOMER"/><el-option label="销售发言" value="SELLER"/><el-option label="沟通纪要" value="NOTE"/></el-select><el-date-picker v-model="conversation.occurredAt" type="datetime" placeholder="沟通时间" :clearable="false"/></div><el-input v-model="conversation.content" type="textarea" :rows="4" maxlength="12000" show-word-limit placeholder="粘贴对话或记录本次沟通。请选择正确角色，帮助 AI 区分客户诉求与销售介绍。"/><div class="compose-footer"><small>只分析已录入或已同步的内容。</small><el-button type="primary" :loading="busy" @click="addConversation">记录沟通</el-button></div></div>
              <el-empty v-if="!data.activities.length" description="还没有沟通记录"/>
              <article v-for="a in data.activities" :key="a.id" class="activity"><div class="activity-meta"><b>{{ roles[a.role] || '记录' }}</b><span>{{ date(a.occurredAt) }}</span><el-tag v-if="a.type === 'ARCHIVE_MESSAGE'" size="small">企微存档</el-tag></div><p>{{ a.content }}</p></article>
            </el-tab-pane>
            <el-tab-pane label="AI 建议" name="ai">
              <el-alert v-if="!catalog.products.length" title="尚未配置产品目录，AI 将辅助澄清需求，不能提供具体产品报价或资料。" type="info" :closable="false"/>
              <div v-if="generating" class="generating">正在{{ skillTitles[generating] }}，请稍候…</div>
              <el-empty v-if="!aiRuns.length && !generating" description="先记录沟通，再选择上方的销售能力"/>
              <article v-for="run in aiRuns" :key="run.id" class="ai-card"><header><b>{{ skillTitles[run.skillId] }}</b><span>{{ date(run.createdAt) }}</span><el-tag v-if="run.appliedAt" type="success" size="small">已采纳</el-tag><el-tag v-else-if="run.stale" type="warning" size="small">上下文已变化</el-tag></header>
                <el-alert v-if="run.status === 'FAILED'" :title="skillError(run.errorCode)" type="warning" :closable="false"/>
                <p v-else-if="run.status === 'RUNNING'">正在生成；若请求中断，可刷新查看或重新发起。</p>
                <template v-else-if="run.status === 'SUCCEEDED'">
                  <template v-if="run.skillId === 'analyze-lead'"><dl><dt>需求判断</dt><dd>{{ run.output.needs || '尚不明确' }}</dd><dt>主要顾虑</dt><dd>{{ run.output.concerns || '尚不明确' }}</dd><dt>阶段建议</dt><dd>{{ stages[run.output.stageSuggestion] || '待确认' }}（请在档案中人工确认）</dd></dl><p v-for="q in run.output.missingInfo" :key="q" class="question">待确认：{{ q }}</p></template>
                  <template v-if="run.skillId === 'suggest-reply'"><el-input v-model="run.output.reply" type="textarea" :autosize="{minRows:3,maxRows:12}"/><div class="reply-tools"><el-button size="small" @click="copy(run.output.reply)">复制回复</el-button><small>复制和采纳均不代表已发送</small></div><p v-for="q in run.output.questions" :key="q" class="question">待确认：{{ q }}</p><a v-for="m in materials(run.output.materialIds || [])" :key="m.id" :href="m.url" target="_blank" rel="noopener noreferrer" class="material">资料：{{ m.title }}</a></template>
                  <template v-if="run.skillId === 'plan-followup'"><h3>{{ run.output.action }}</h3><p>{{ run.output.reason }}</p><div class="due">建议时间：{{ date(run.output.dueAt) }}</div></template>
                  <el-collapse class="evidence"><el-collapse-item title="查看判断依据" name="evidence"><p v-for="ref in run.output.evidenceIds" :key="ref">{{ evidence(run, ref) }}</p></el-collapse-item></el-collapse>
                  <el-button size="small" type="primary" plain :loading="busy" :disabled="!!run.appliedAt || run.stale" @click="apply(run)">{{ run.skillId === 'plan-followup' ? '采纳并创建任务' : run.skillId === 'analyze-lead' ? '采纳需求与顾虑' : '标记草稿已采纳' }}</el-button>
                </template>
              </article>
            </el-tab-pane>
            <el-tab-pane label="跟进任务" name="tasks"><div class="tab-toolbar"><span>复制回复后，记得记录真实沟通结果。</span><el-button :disabled="!canFollow" @click="showTask = true">＋ 安排跟进</el-button></div><el-empty v-if="!data.tasks.length" description="暂无跟进任务"/><article v-for="t in data.tasks" :key="t.id" class="task-card"><header><b>{{ t.action }}</b><el-tag :type="t.status === 'DONE' ? 'success' : t.status === 'CANCELLED' ? 'info' : 'warning'">{{ taskStatuses[t.status] }}</el-tag></header><p>{{ t.reason }}</p><small>{{ date(t.dueAt) }}</small><p v-if="t.resultNote" class="result">{{ t.resultNote }}</p><div v-if="t.status === 'PENDING'" class="task-actions"><el-button size="small" type="primary" :disabled="busy" @click="complete(t)">记录结果并完成</el-button><el-button size="small" :disabled="busy" @click="selectedTask = t; postponeAt = new Date(t.dueAt); showPostpone = true">延期</el-button><el-button size="small" text :disabled="busy" @click="cancel(t)">取消</el-button></div></article></el-tab-pane>
            <el-tab-pane label="实收记录" name="receipts"><div class="tab-toolbar"><span>以实际收款为准，作废只用于录入纠错。</span><el-button @click="showReceipt = true">＋ 登记实收</el-button></div><el-empty v-if="!data.receipts.length" description="暂无实收记录；意向不等于成交"/><article v-for="r in data.receipts" :key="r.id" class="receipt-card"><div><strong>¥{{ money(r.amount) }}</strong><el-tag :type="r.status === 'CONFIRMED' ? 'success' : 'info'">{{ r.status === 'CONFIRMED' ? '有效实收' : '已作废' }}</el-tag><p>{{ date(r.paidAt) }} · 记录人 {{ r.actor }}</p><small v-if="r.receiptRef">凭证：{{ r.receiptRef }}</small><p v-if="r.voidReason">作废原因：{{ r.voidReason }}</p></div><el-button v-if="r.status === 'CONFIRMED'" text :disabled="busy" @click="voidReceipt(r)">作废纠错</el-button></article></el-tab-pane>
          </el-tabs></div>
        </section>
      </div>
      <el-dialog v-model="showTask" title="安排下一次跟进" width="480px"><el-form label-position="top"><el-form-item label="跟进动作" required><el-input v-model="taskForm.action" maxlength="1000" placeholder="例如：确认试用反馈，补充适用案例"/></el-form-item><el-form-item label="原因"><el-input v-model="taskForm.reason" type="textarea" maxlength="2000"/></el-form-item><el-form-item label="跟进时间" required><el-date-picker v-model="taskForm.dueAt" type="datetime" :clearable="false"/></el-form-item></el-form><template #footer><el-button @click="showTask = false">取消</el-button><el-button type="primary" :loading="busy" @click="createTask">创建任务</el-button></template></el-dialog>
      <el-dialog v-model="showPostpone" title="调整跟进时间" width="400px"><el-date-picker v-model="postponeAt" type="datetime" :clearable="false"/><template #footer><el-button type="primary" :loading="busy" @click="postpone">保存时间</el-button></template></el-dialog>
      <el-dialog v-model="showReceipt" title="登记实际收款" width="480px"><el-alert title="登记后客户转为已成交，未完成的转化跟进任务将取消。" type="info" :closable="false"/><el-form label-position="top" class="receipt-form"><el-form-item label="产品（可选）"><el-select v-model="receiptForm.productId" clearable><el-option v-for="p in catalog.products" :key="p.id" :label="p.name" :value="p.id"/></el-select></el-form-item><el-form-item label="实收金额（人民币）" required><el-input-number v-model="receiptForm.amount" :precision="2" :min="0.01" :max="999999999999.99"/></el-form-item><el-form-item label="收款时间" required><el-date-picker v-model="receiptForm.paidAt" type="datetime" :clearable="false"/></el-form-item><el-form-item label="凭证号（可选）"><el-input v-model="receiptForm.receiptRef" maxlength="180"/></el-form-item></el-form><template #footer><el-button @click="showReceipt = false">取消</el-button><el-button type="primary" :loading="busy" @click="saveReceipt">确认已收款并登记</el-button></template></el-dialog>
    </template>
  </main>
</template>

<style scoped>
.lead-page{padding:24px;max-width:1600px;margin:auto;color:#203745}.lead-header{display:flex;justify-content:space-between;align-items:center;margin:25px 0;gap:20px}.identity{display:flex;align-items:center;gap:16px}.avatar{font-size:26px;padding:16px 20px;border-radius:16px;background:#e2f1ec;color:#177c69}h1{font-size:26px;margin:0 0 8px;display:flex;align-items:center;gap:14px}.identity p{margin:0;color:#86959d;font-size:12px}.lead-layout{display:grid;grid-template-columns:300px minmax(0,1fr);gap:22px;margin-top:20px;align-items:start}.panel{background:var(--BgWhite,#fff);border:1px solid #e5ebee;border-radius:14px;padding:22px}.panel-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:22px}h2{font-size:17px;margin:0}.panel-heading span{font-size:11px;color:#87979e}.main-column{min-width:0}.copilot-card{padding:25px;border-radius:14px;background:linear-gradient(110deg,#153d3d,#215c53);color:#fff;margin-bottom:20px}.eyebrow{font-size:10px;letter-spacing:2px;color:#abd7c9;margin-bottom:10px}.copilot-card h2{font-size:22px}.copilot-card p{font-size:13px;color:#c0d6d1;margin:10px 0 22px}.skill-buttons{display:flex;flex-wrap:wrap;gap:8px}.skill-buttons .el-button{margin:0;background:#fff;border:none;color:#234c45}.detail-panel{padding-top:8px}.compose-controls{display:flex;gap:10px;margin-bottom:12px;flex-wrap:wrap}.conversation-compose{background:#f6f9fa;border-radius:10px;padding:16px;margin:12px 0 24px}.compose-footer{display:flex;justify-content:space-between;align-items:center;margin-top:12px;gap:12px}.compose-footer small{font-size:11px;color:#81949e}.activity{border-left:2px solid #e7efeb;padding:0 0 22px 20px;margin-left:7px}.activity-meta{display:flex;align-items:center;gap:12px;font-size:12px}.activity-meta span{color:#8b99a1}.activity p,.evidence p{white-space:pre-wrap;line-height:1.8;font-size:13px;color:#536772}.activity p{margin-bottom:0}.ai-card,.task-card{border:1px solid #e5ebee;border-radius:10px;margin:16px 0;padding:20px}.ai-card header,.task-card header{display:flex;align-items:center;gap:10px;margin-bottom:16px}.ai-card header span{color:#8898a1;font-size:11px;margin-left:auto}.ai-card dl{display:grid;grid-template-columns:80px 1fr;gap:14px;font-size:13px;line-height:1.7}.ai-card dt{color:#84959f}.ai-card dd{margin:0;white-space:pre-wrap}.ai-card h3{font-size:16px}.ai-card p,.task-card p{font-size:13px;line-height:1.8;white-space:pre-wrap}.question{background:#f6f8f9;padding:8px 12px;color:#748894;font-size:12px!important;border-radius:6px}.evidence{margin:16px 0}.due{font-size:12px;color:#278574;background:#eef7f3;padding:10px;border-radius:6px}.reply-tools{display:flex;align-items:center;gap:12px;margin-top:12px}.reply-tools small{color:#8b99a1;font-size:11px}.material{display:block;color:#278574;font-size:13px;margin-top:8px}.tab-toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:16px 0}.tab-toolbar span{font-size:12px;color:#8799a3}.task-actions{margin-top:18px}.task-card small{color:#8799a3}.result,.product-note{background:#f1f7f5;padding:12px;border-radius:6px;font-size:12px}.product-note{margin-bottom:20px}.receipt-card{display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #edf1f3;padding:24px 0}.receipt-card strong{font-size:24px;margin-right:15px}.receipt-card p,.receipt-card small{font-size:12px;color:#80949f}.receipt-form{margin-top:22px}.generating{padding:24px;text-align:center;color:#288571}@media(max-width:1050px){.lead-layout{grid-template-columns:1fr}.profile-card .el-form{display:grid;grid-template-columns:1fr 1fr;gap:0 20px}.lead-page{padding:16px}.lead-header{align-items:flex-start}.identity p{max-width:400px}}@media(max-width:650px){.profile-card .el-form{display:block}.lead-header{flex-direction:column}.compose-footer,.tab-toolbar{align-items:flex-start;flex-direction:column}}
</style>
