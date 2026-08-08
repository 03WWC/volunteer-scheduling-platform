<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MagicStick, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { activityApi, positionApi, scheduleApi } from '@/api/modules'
import type {
  ActivityRecord,
  ActivitySignupRecord,
  PageResult,
  PositionRecord,
  ScheduleAssignmentRecord,
  ScheduleDetailRecord,
} from '@/types/api'

const loading = ref(false)
const activityLoading = ref(false)
const activityId = ref<number>()
const activities = ref<ActivityRecord[]>([])
const detail = ref<ScheduleDetailRecord>()
const positions = ref<PositionRecord[]>([])
const signups = ref<ActivitySignupRecord[]>([])
const assignments = computed<ScheduleAssignmentRecord[]>(() => detail.value?.assignments || [])
const selectedActivity = computed(() => activities.value.find((item) => item.id === activityId.value))
const approvedSignups = computed(() => signups.value.filter((item) => item.signupStatus === 'APPROVED'))

onMounted(loadActivities)

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 100 })
    activities.value = Array.isArray(result) ? result : (result as PageResult<ActivityRecord>).records || []
    if (!activityId.value && activities.value.length) {
      activityId.value = activities.value[0].id
      await loadDetail()
    }
  } finally {
    activityLoading.value = false
  }
}

async function loadDetail() {
  if (!activityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  loading.value = true
  try {
    await loadScheduleContext(activityId.value)
    detail.value = await scheduleApi.detail(activityId.value, { silentError: true }).catch(() => undefined)
  } finally {
    loading.value = false
  }
}

async function autoGenerate() {
  if (!activityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  await loadScheduleContext(activityId.value)
  if (!positions.value.length) {
    ElMessage.warning('当前活动还没有岗位，请先到“岗位管理”新增岗位')
    return
  }
  if (!approvedSignups.value.length) {
    ElMessage.warning('当前活动没有已通过报名，请先到“报名审核”通过志愿者报名')
    return
  }
  loading.value = true
  try {
    const activityName = selectedActivity.value?.name || `活动 ${activityId.value}`
    detail.value = await scheduleApi.autoGenerate({ activityId: activityId.value, planName: `${activityName} 自动排班`, generatedBy: 1 })
    ElMessage.success('排班已生成')
  } finally {
    loading.value = false
  }
}

async function loadScheduleContext(id: number) {
  const [positionResult, signupResult] = await Promise.all([
    positionApi.list(id).catch(() => []),
    activityApi.signups({ activityId: id }).catch(() => []),
  ])
  positions.value = positionResult
  signups.value = signupResult
}

async function publish() {
  if (!detail.value?.planId) {
    ElMessage.warning('请先查询或生成排班')
    return
  }
  detail.value = await scheduleApi.publish(detail.value.planId)
  ElMessage.success('排班已发布')
}

function fmt(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="filter-row">
        <el-select
          v-model="activityId"
          filterable
          :loading="activityLoading"
          placeholder="选择活动"
          style="width: 280px"
          @change="loadDetail"
        >
          <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
        </el-select>
        <el-button :icon="Search" @click="loadDetail">查询详情</el-button>
        <el-button type="primary" :icon="MagicStick" @click="autoGenerate">自动排班</el-button>
      </div>
      <div class="command-row">
        <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
        <el-button type="success" :icon="Upload" @click="publish">发布排班</el-button>
      </div>
    </div>

    <el-alert
      class="schedule-prerequisite"
      type="info"
      :closable="false"
      show-icon
      :title="`排班条件：岗位 ${positions.length} 个，报名 ${signups.length} 条，已通过 ${approvedSignups.length} 条`"
      description="自动排班只会从已审核通过的报名志愿者中选择人员；如果已通过为 0，请先到报名审核处理。"
    />

    <div class="table-panel">
      <div class="table-meta">
        <span>{{ detail?.planName || '排班计划' }} · {{ detail?.planStatus || '未查询' }}</span>
        <small>计划编号 {{ detail?.planNo || '-' }}，共 {{ assignments.length }} 个岗位安排</small>
      </div>
      <el-table v-loading="loading" :data="assignments" row-key="id">
        <el-table-column prop="id" label="安排编号" width="110" />
        <el-table-column prop="areaId" label="区域编号" width="110" />
        <el-table-column prop="positionId" label="岗位编号" width="110" />
        <el-table-column prop="userId" label="志愿者编号" width="125" />
        <el-table-column prop="workDate" label="服务日期" width="125" />
        <el-table-column label="服务时间" min-width="220">
          <template #default="{ row }">{{ fmt(row.startTime) }} - {{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="assignmentStatus" label="状态" width="120" />
      </el-table>
    </div>
  </section>
</template>
