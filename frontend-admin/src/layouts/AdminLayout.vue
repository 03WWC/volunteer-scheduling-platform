<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Calendar,
  Check,
  DataAnalysis,
  Fold,
  Location,
  Message,
  Money,
  Operation,
  Setting,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)
const activePath = computed(() => route.path)
const pageTitle = computed(() => String(route.meta.title || '调度总览'))
const canViewActivityGroup = computed(() =>
  ['activity:manage', 'signup:review', 'area:manage', 'position:manage'].some(authStore.hasPermission),
)
const canViewDispatchGroup = computed(() => authStore.hasPermission('dispatch:manage'))

function logout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="admin-shell" :class="{ 'is-collapsed': collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">V</span>
        <div v-if="!collapsed" class="brand-copy">
          <strong>志愿调度</strong>
          <span>运营管理平台</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="activePath" router :collapse="collapsed">
          <el-menu-item index="/dashboard"><el-icon><DataAnalysis /></el-icon><template #title>调度总览</template></el-menu-item>
          <el-sub-menu v-if="canViewActivityGroup" index="activity">
            <template #title><el-icon><Calendar /></el-icon><span>活动资源</span></template>
            <el-menu-item v-if="authStore.hasPermission('activity:manage')" index="/activity/list">活动管理</el-menu-item>
            <el-menu-item v-if="authStore.hasPermission('signup:review')" index="/activity/signups">报名审核</el-menu-item>
            <el-menu-item v-if="authStore.hasPermission('area:manage')" index="/activity/area">服务区域</el-menu-item>
            <el-menu-item v-if="authStore.hasPermission('position:manage')" index="/activity/position">岗位管理</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-if="authStore.hasPermission('volunteer:manage')" index="/user/volunteers"><el-icon><User /></el-icon><template #title>志愿者管理</template></el-menu-item>
          <el-menu-item v-if="authStore.hasPermission('schedule:manage')" index="/schedule/plans"><el-icon><Operation /></el-icon><template #title>排班计划</template></el-menu-item>
          <el-sub-menu v-if="canViewDispatchGroup" index="dispatch">
            <template #title><el-icon><Location /></el-icon><span>智能调度</span></template>
            <el-menu-item index="/dispatch/tasks">调度任务</el-menu-item>
            <el-menu-item index="/dispatch/shortage">缺口预警</el-menu-item>
            <el-menu-item index="/dispatch/ai-prediction">AI 风险预测</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-if="authStore.hasPermission('checkin:manage')" index="/location/checkins"><el-icon><Check /></el-icon><template #title>签到记录</template></el-menu-item>
          <el-menu-item v-if="authStore.hasPermission('settlement:manage')" index="/settlement/bills"><el-icon><Money /></el-icon><template #title>工时结算</template></el-menu-item>
          <el-menu-item v-if="authStore.hasPermission('message:manage')" index="/message/notices"><el-icon><Message /></el-icon><template #title>通知中心</template></el-menu-item>
          <el-menu-item v-if="authStore.hasPermission('system:manage')" index="/system/admins"><el-icon><Setting /></el-icon><template #title>系统管理</template></el-menu-item>
        </el-menu>
      </el-scrollbar>

      <button class="collapse-button" type="button" title="收起导航" @click="collapsed = !collapsed">
        <el-icon><Fold /></el-icon>
        <span v-if="!collapsed">收起导航</span>
      </button>
    </aside>

    <section class="main-area">
      <header class="topbar">
        <div>
          <span class="eyebrow">志愿者智能调度平台</span>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <div class="system-state"><span></span>服务正常</div>
          <el-button text circle title="消息提醒"><el-icon><Bell /></el-icon></el-button>
          <el-dropdown>
            <button class="profile-trigger" type="button">
              <span class="avatar">{{ authStore.displayName.slice(0, 1) }}</span>
              <span class="profile-name">{{ authStore.displayName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item><el-icon><Setting /></el-icon>账号设置</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
</template>
