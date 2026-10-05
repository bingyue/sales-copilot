<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Cookies from 'js-cookie'
import { login } from './api'
import { setToken } from '@/utils/auth'
const route = useRoute(), router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false), formRef = ref()
const rules = { username: [{ required: true, message: '请输入账号', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }] }
onMounted(() => { Cookies.remove('password'); Cookies.remove('rememberMe') })
async function submit() {
  if (loading.value || !(await formRef.value.validate().catch(() => false))) return
  loading.value = true
  try { const result = await login(form); setToken(result.data.token); const redirect = String(route.query.redirect || '/sales/followups'); await router.push(redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/sales/followups') }
  catch {} finally { loading.value = false }
}
</script>
<template>
  <main class="login-shell">
    <section class="brand-story">
      <div class="brand"><img :src="'/tools/static/logo.svg'" alt="集智销伴 Logo"/><div><b>集智销伴</b><span>JIZHI SALES COPILOT</span></div></div>
      <div class="story"><div class="eyebrow">YOUR NEXT STEP IN SALES</div><h1>好的沟通，<br/>值得一次<br/><em>恰到好处的跟进。</em></h1><p>记住客户的需求，整理每次沟通。<br/>让 AI 辅助判断，让你专注建立信任。</p>
        <div class="journey"><span>理解客户</span><i>→</i><span>持续跟进</span><i>→</i><span>记录成交</span></div>
      </div><div class="story-footer">每个意向客户，都有下一步。</div>
    </section>
    <section class="login-area"><div class="login-card"><span class="welcome">欢迎回来</span><h2>进入销售工作台</h2><p>客户、AI 建议与跟进任务，都在这里。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
        <el-form-item label="账号" prop="username"><el-input v-model.trim="form.username" placeholder="请输入管理员账号" autocomplete="username"/></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" @keyup.enter="submit"/></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="login-button">{{ loading ? '正在登录…' : '登录工作台 →' }}</el-button>
      </el-form><div class="login-note">使用部署时设置的内部管理员账号登录。</div>
    </div><footer>Jizhi Sales Copilot · 基于源雀 SCRM 构建</footer></section>
  </main>
</template>
<style scoped>
.login-shell{display:grid;grid-template-columns:1.05fr 1fr;min-height:100vh;background:#f7f9f8;color:#233d43}.brand-story{background:#173f39;color:#f4f8f3;padding:48px 9%;position:relative;display:flex;flex-direction:column;overflow:hidden}.brand-story:after{content:'';position:absolute;width:450px;height:450px;border:1px solid #42665a;border-radius:50%;right:-275px;bottom:-110px;box-shadow:0 0 0 70px #ffffff03,0 0 0 140px #ffffff03;pointer-events:none}.brand{display:flex;align-items:center;gap:14px}.brand img{width:48px;height:48px}.brand b{font-size:22px;display:block;letter-spacing:2px}.brand span{display:block;font-size:9px;letter-spacing:2px;margin-top:5px;color:#a2bdb0}.story{margin:auto 0;padding:70px 0}.eyebrow{font-size:10px;letter-spacing:2px;color:#a2c6b5}h1{font-size:clamp(34px,3.5vw,58px);line-height:1.45;font-weight:550;letter-spacing:-1px;margin:25px 0}h1 em{color:#d9c680;font-style:normal}.story p{font-size:15px;line-height:2;color:#b3c9c0}.journey{display:flex;align-items:center;gap:15px;margin-top:35px;font-size:12px;color:#c3d9ce}.journey span{border:1px solid #527366;padding:9px 12px;border-radius:7px}.journey i{font-style:normal;color:#6d9582}.story-footer{font-size:11px;color:#8aab9b;letter-spacing:1px}.login-area{display:flex;align-items:center;justify-content:center;position:relative;padding:60px 30px}.login-card{width:360px;max-width:100%}.welcome{font-size:12px;color:#8faaa1;letter-spacing:2px}.login-card h2{font-size:30px;font-weight:600;letter-spacing:-1px;margin:16px 0 12px}.login-card>p{font-size:13px;color:#8b9b9e;margin:0 0 35px}.login-button{width:100%;margin-top:14px;background:#216e5c;border-color:#216e5c;height:47px}.login-note{font-size:11px;color:#9aabaa;margin-top:22px;text-align:center}.login-area footer{position:absolute;bottom:30px;font-size:10px;color:#9badaa}.login-card :deep(.el-input__wrapper){background:#fff;box-shadow:0 0 0 1px #dce6e2 inset}.login-card :deep(.el-form-item__label){font-size:12px;color:#526c6d}@media(max-width:800px){.login-shell{grid-template-columns:1fr}.brand-story{padding:25px 30px}.story,.story-footer{display:none}.login-area{min-height:calc(100vh - 100px);padding:50px 25px 80px}}
</style>
