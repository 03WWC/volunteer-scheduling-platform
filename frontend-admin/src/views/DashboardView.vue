<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Calendar, Check, Clock, Operation, User } from '@element-plus/icons-vue'
import { activityApi, locationApi, positionApi, scheduleApi, volunteerApi } from '@/api/modules'
import type { ActivityRecord, CheckinRecord, PageResult, PositionRecord, ScheduleAssignmentRecord, ScheduleDetailRecord, VolunteerRecord } from '@/types/api'
import { resolveAlertRoute, resolveMetricRoute } from './dashboardActions'

const router = useRouter()
const loading = ref(false)
const activities = ref<ActivityRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const schedules = ref<ScheduleDetailRecord[]>([])
const positions = ref<PositionRecord[]>([])
const checkins = ref<CheckinRecord[]>([])

const allAssignments = computed(() => schedules.value.flatMap((item) => item.assignments || []))
const checkedInUserCount = computed(() => new Set(checkins.value
  .filter((item) => item.checkinStatus === 'NORMAL' && item.checkinType === 'CHECK_IN')
  .map((item) => item.userId)).size)
const requiredCount = computed(() => positions.value.reduce((sum, item) => sum + Number(item.needCount || 0), 0))
const assignedCount = computed(() => allAssignments.value.length)
const shortageCount = computed(() => Math.max(requiredCount.value - assignedCount.value, 0))
const totalWorkHours = computed(() => {
  const minutes = allAssignments.value.reduce((sum, item) => sum + assignmentMinutes(item), 0)
  return Math.round(minutes / 60)
})

const stats = computed(() => [
  { label: '活动总数', value: String(activities.value.length), delta: '来自活动服务', icon: Calendar, tone: 'green' },
  { label: '在岗志愿者', value: String(checkedInUserCount.value), delta: `志愿者库 ${volunteers.value.length} 人`, icon: User, tone: 'blue' },
  { label: '待处理缺口', value: String(shortageCount.value), delta: `需求 ${requiredCount.value} / 已排 ${assignedCount.value}`, icon: Operation, tone: shortageCount.value > 0 ? 'red' : 'green' },
  { label: '计划服务时长', value: `${totalWorkHours.value}h`, delta: '按排班计划统计', icon: Clock, tone: 'amber' },
])

const scheduleRows = computed(() => activities.value.slice(0, 6).map((activity) => {
  const detail = schedules.value.find((item) => item.activityId === activity.id)
  const assignments = detail?.assignments || []
  const activityPositions = positions.value.filter((item) => item.activityId === activity.id)
  const needCount = activityPositions.reduce((sum, item) => sum + Number(item.needCount || 0), 0)
  const firstAssignment = assignments[0]
  return {
    time: fmtTime(firstAssignment?.startTime || activity.startTime),
    name: activity.name,
    area: activity.location || activity.address || '-',
    people: `${assignments.length} / ${needCount || '-'}`,
    status: resolveScheduleStatus(assignments.length, needCount, detail?.planStatus),
  }
}))

const alerts = computed(() => {
  const list = []
  if (shortageCount.value > 0) {
    list.push({ title: `当前岗位总缺口 ${shortageCount.value} 人`, time: '实时计算', level: '紧急' })
  }
  const pendingCheckinCount = Math.max(assignedCount.value - checkedInUserCount.value, 0)
  if (pendingCheckinCount > 0) {
    list.push({ title: `${pendingCheckinCount} 个排班人员尚未签到`, time: '来自签到记录', level: '提醒' })
  }
  if (!list.length) {
    list.push({ title: '当前活动、排班和签到数据整体稳定', time: '实时计算', level: '待办' })
  }
  return list
})

const coverageRows = computed(() => activities.value.slice(0, 4).map((activity) => {
  const detail = schedules.value.find((item) => item.activityId === activity.id)
  const assignments = detail?.assignments || []
  const needCount = positions.value
    .filter((item) => item.activityId === activity.id)
    .reduce((sum, item) => sum + Number(item.needCount || 0), 0)
  const percentage = needCount ? Math.min(Math.round(assignments.length * 100 / needCount), 100) : 0
  return { name: activity.name, percentage }
}))
const dashboardSummary = computed(() => `共 ${activities.value.length} 场活动 · ${assignedCount.value} 个排班安排`)

onMounted(loadDashboard)

async function loadDashboard() {
  loading.value = true
  try {
    const activityResult = await activityApi.page({ pageNo: 1, pageSize: 20 })
    activities.value = Array.isArray(activityResult) ? activityResult : (activityResult as PageResult<ActivityRecord>).records || []
    volunteers.value = await volunteerApi.list({ pageNo: 1, pageSize: 200 })
    const details = await Promise.all(activities.value.map((activity) =>
      scheduleApi.detail(activity.id, { silentError: true }).catch(() => undefined)))
    schedules.value = details.filter((item): item is ScheduleDetailRecord => Boolean(item))
    const positionResults = await Promise.all(activities.value.map((activity) => positionApi.list(activity.id).catch(() => [])))
    positions.value = positionResults.flat()
    const checkinResults = await Promise.all(activities.value.map((activity) => locationApi.checkins({ activityId: activity.id }).catch(() => [])))
    checkins.value = checkinResults.flat()
  } finally {
    loading.value = false
  }
}

function assignmentMinutes(item: ScheduleAssignmentRecord) {
  if (!item.startTime || !item.endTime) {
    return 0
  }
  return Math.max((new Date(item.endTime).getTime() - new Date(item.startTime).getTime()) / 60000, 0)
}

function resolveScheduleStatus(assigned: number, need: number, planStatus?: string) {
  if (need && assigned < need) {
    return '待补员'
  }
  if (planStatus === 'PUBLISHED') {
    return '执行中'
  }
  return assigned ? '已就绪' : '待排班'
}

function fmtTime(value?: string) {
  return value ? value.replace('T', ' ').slice(11, 16) : '-'
}

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="dashboard">
    <div class="summary-grid">
      <button
        v-for="item in stats"
        :key="item.label"
        type="button"
        class="metric-card metric-action"
        @click="go(resolveMetricRoute(item.label))"
      >
        <div class="metric-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></div>
        <div><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.delta }}</small></div>
      </button>
    </div>

    <div class="dashboard-grid">
      <section class="panel schedule-panel">
        <div class="panel-heading">
          <div><h2>排班运行</h2><p>{{ dashboardSummary }}</p></div>
          <el-button text type="primary" @click="go('/schedule/plans')">查看全部<el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <el-table v-loading="loading" :data="scheduleRows" stripe>
          <el-table-column prop="time" label="开始" width="88" />
          <el-table-column prop="name" label="活动" min-width="190" />
          <el-table-column prop="area" label="区域" min-width="110" />
          <el-table-column prop="people" label="到岗/需求" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === '待补员' ? 'danger' : row.status === '执行中' ? 'success' : 'info'" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel alert-panel">
        <div class="panel-heading"><div><h2>待处理事项</h2><p>按紧急程度排序</p></div><span class="count-badge">{{ alerts.length }}</span></div>
        <div class="alert-list">
          <button v-for="item in alerts" :key="item.title" type="button" class="alert-item" @click="go(resolveAlertRoute(item.title))">
            <span class="alert-dot" :class="item.level"></span>
            <span class="alert-copy"><strong>{{ item.title }}</strong><small>{{ item.time }}</small></span>
            <el-tag size="small" :type="item.level === '紧急' ? 'danger' : 'warning'">{{ item.level }}</el-tag>
          </button>
        </div>
      </section>
    </div>

    <section class="panel progress-panel">
      <div class="panel-heading"><div><h2>区域岗位满足率</h2><p>当前活动岗位覆盖情况</p></div><el-tag type="success"><el-icon><Check /></el-icon>整体稳定</el-tag></div>
      <div class="progress-grid">
        <div v-for="item in coverageRows" :key="item.name"><span>{{ item.name }} <b>{{ item.percentage }}%</b></span><el-progress :percentage="item.percentage" :show-text="false" :color="item.percentage >= 95 ? '#258a72' : item.percentage >= 80 ? '#d18c28' : '#cf4d45'" /></div>
        <div v-if="!coverageRows.length"><span>暂无活动 <b>0%</b></span><el-progress :percentage="0" :show-text="false" color="#d18c28" /></div>
      </div>
    </section>
  </div>
</template>
