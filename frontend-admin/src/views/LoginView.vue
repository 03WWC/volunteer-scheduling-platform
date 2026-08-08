<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({ account: 'admin', password: 'Admin123456' })

async function submit() {
  if (!form.account.trim() || !form.password.trim()) return
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-brand">
      <div class="login-brand-inner">
        <span class="brand-kicker">VOLUNTEER OPERATIONS</span>
        <h1>志愿者智能调度平台</h1>
        <p>让活动、岗位、人员和签到结算保持在同一套节奏里。</p>
        <div class="login-metrics">
          <div><strong>24</strong><span>今日活动</span></div>
          <div><strong>386</strong><span>在岗志愿者</span></div>
          <div><strong>98.6%</strong><span>岗位满足率</span></div>
        </div>
      </div>
    </section>
    <section class="login-panel">
      <form class="login-form" @submit.prevent="submit">
        <div class="login-heading">
          <span class="login-logo">V</span>
          <div><h2>管理端登录</h2><p>使用平台管理账号进入</p></div>
        </div>
        <label>账号</label>
        <el-input v-model="form.account" size="large" :prefix-icon="User" autocomplete="username" />
        <label>密码</label>
        <el-input
          v-model="form.password"
          size="large"
          type="password"
          show-password
          :prefix-icon="Lock"
          autocomplete="current-password"
          @keyup.enter="submit"
        />
        <el-button native-type="submit" type="primary" size="large" :loading="loading">登录平台</el-button>
        <div class="login-footer">内部运营系统 · 访问行为将被记录</div>
      </form>
    </section>
  </div>
</template>
