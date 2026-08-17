<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MagicStick, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, areaApi, positionApi, scheduleApi, volunteerApi } from '@/api/modules'
import type {
  ActivityRecord,
  ActivitySignupRecord,
  AreaRecord,
  PageResult,
  PositionRecord,
  ScheduleAssignmentRecord,
  ScheduleDetailRecord,
  VolunteerRecord,
} from '@/types/api'

const loading = ref(false)
const activityLoading = ref(false)
const activityId = ref<number>()
const activities = ref<ActivityRecord[]>([])
const detail = ref<ScheduleDetailRecord>()
const areas = ref<AreaRecord[]>([])
const positions = ref<PositionRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const signups = ref<ActivitySignupRecord[]>([])
const assignments = computed<ScheduleAssignmentRecord[]>(() => detail.value?.assignments || [])
const selectedActivity = computed(() => activities.value.find((item) => item.id === activityId.value))
const approvedSignups = computed(() => signups.value.filter((item) => item.signupStatus === 'APPROVED'))
const canAutoGenerate = computed(() => !loading.value && !assignments.value.length)
const areaNameMap = computed(() => new Map(areas.value.map((item) => [item.id, item.name])))
const positionNameMap = computed(() => new Map(positions.value.map((item) => [item.id, item.name])))
const volunteerNameMap = computed(() => new Map(volunteers.value.map((item) => [
  item.id,
  item.realName || item.nickname || item.username || `志愿者 ${item.id}`,
])))

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
  if (assignments.value.length) {
    ElMessage.info('当前活动已有排班安排，不会重复生成。需要重排时请先清理原计划。')
    return
  }
  loading.value = true
  try {
    const activityName = selectedActivity.value?.name || `活动 ${activityId.value}`
    detail.value = await scheduleApi.autoGenerate(
      { activityId: activityId.value, planName: `${activityName} 自动排班`, generatedBy: 1 },
      { silentError: true },
    )
    ElMessage.success('排班已生成')
  } catch (error) {
    await showAutoGenerateFailure(error)
  } finally {
    loading.value = false
  }
}

async function showAutoGenerateFailure(error: unknown) {
  await ElMessageBox.alert(getErrorMessage(error), '自动排班失败诊断', {
    confirmButtonText: '知道了',
    type: 'warning',
    customClass: 'schedule-diagnosis-dialog',
  }).catch(() => undefined)
}

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response
    if (response?.data?.message) {
      return response.data.message
    }
  }
  return error instanceof Error ? error.message : '自动排班失败，请检查岗位人数、报名审核、技能标签和服务时间。'
}

async function loadScheduleContext(id: number) {
  const [areaResult, positionResult, signupResult, volunteerResult] = await Promise.all([
    areaApi.list(id).catch(() => []),
    positionApi.list(id).catch(() => []),
    activityApi.signups({ activityId: id }).catch(() => []),
    volunteerApi.list({ pageNo: 1, pageSize: 500 }).catch(() => []),
  ])
  areas.value = areaResult
  positions.value = positionResult
  signups.value = signupResult
  volunteers.value = volunteerResult
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

function areaLabel(areaId?: number) {
  if (!areaId) {
    return '未指定区域'
  }
  return areaNameMap.value.get(areaId) || `区域 ${areaId}`
}

function positionLabel(positionId?: number) {
  if (!positionId) {
    return '未指定岗位'
  }
  return positionNameMap.value.get(positionId) || `岗位 ${positionId}`
}

function volunteerLabel(userId?: number) {
  if (!userId) {
    return '未指定志愿者'
  }
  return volunteerNameMap.value.get(userId) || `志愿者 ${userId}`
}

function assignmentStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    WAIT_CONFIRM: '待确认',
    CONFIRMED: '已确认',
  }
  return status ? labels[status] || status : '-'
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
        <el-button type="primary" :icon="MagicStick" :disabled="!canAutoGenerate" :loading="loading" @click="autoGenerate">自动排班</el-button>
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
    <el-alert
      v-if="assignments.length"
      class="schedule-prerequisite"
      type="success"
      :closable="false"
      show-icon
      title="当前活动已有排班，系统已阻止重复自动排班，避免重复通知志愿者。"
    />

    <div class="table-panel">
      <div class="table-meta">
        <span>{{ detail?.planName || '排班计划' }} · {{ detail?.planStatus || '未查询' }}</span>
        <small>计划编号 {{ detail?.planNo || '-' }}，共 {{ assignments.length }} 个岗位安排</small>
      </div>
      <el-table v-loading="loading" :data="assignments" row-key="id">
        <el-table-column label="安排" width="110">
          <template #default="{ row }">安排 {{ row.id }}</template>
        </el-table-column>
        <el-table-column label="区域" min-width="160">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ areaLabel(row.areaId) }}</strong>
              <span>区域 {{ row.areaId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="岗位" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ positionLabel(row.positionId) }}</strong>
              <span>岗位 {{ row.positionId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="志愿者" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ volunteerLabel(row.userId) }}</strong>
              <span>志愿者 {{ row.userId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="workDate" label="服务日期" width="125" />
        <el-table-column label="服务时间" min-width="220">
          <template #default="{ row }">{{ fmt(row.startTime) }} - {{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">{{ assignmentStatusLabel(row.assignmentStatus) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
:global(.schedule-diagnosis-dialog .el-message-box__message) {
  white-space: pre-line;
  line-height: 1.7;
}
</style>
