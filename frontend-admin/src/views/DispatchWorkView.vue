<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, Connection, Refresh, Search, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { activityApi, areaApi, dispatchApi, locationApi, positionApi, scheduleApi, volunteerApi } from '@/api/modules'
import type {
  ActivityRecord,
  AreaRecord,
  CheckinRecord,
  DispatchRecord,
  PageResult,
  PositionRecord,
  ScheduleAssignmentRecord,
  ScheduleDetailRecord,
  VolunteerRecord,
} from '@/types/api'

interface PositionInsight {
  position: PositionRecord
  areaName: string
  requiredCount: number
  assignedCount: number
  confirmedCount: number
  checkedInCount: number
  scheduleShortage: number
  confirmShortage: number
  checkinShortage: number
  suggestedShortage: number
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW'
  riskText: string
  actionText: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activityLoading = ref(false)
const activityId = ref<number>()
const selectedPositionId = ref<number>()
const resultId = ref<number>()
const activities = ref<ActivityRecord[]>([])
const areas = ref<AreaRecord[]>([])
const positions = ref<PositionRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const detail = ref<ScheduleDetailRecord>()
const checkins = ref<CheckinRecord[]>([])
const tasks = ref<DispatchRecord[]>([])
const selectedTask = ref<DispatchRecord>()
const advancedVisible = ref(false)
const advancedForm = reactive({
  longitude: undefined as number | undefined,
  latitude: undefined as number | undefined,
  radiusMeter: 1000,
  candidateUserIdsText: '',
  requiredCount: undefined as number | undefined,
  reason: '',
  createdBy: 1,
})

const isShortagePage = computed(() => route.meta.module === 'shortage')
const selectedActivity = computed(() => activities.value.find((item) => item.id === activityId.value))
const assignments = computed<ScheduleAssignmentRecord[]>(() => detail.value?.assignments || [])
const recommendations = computed(() => selectedTask.value?.recommendations || [])
const activityNameMap = computed(() => new Map(activities.value.map((activity) => [activity.id, activity.name])))
const areaNameMap = computed(() => new Map(areas.value.map((area) => [area.id, area.name])))
const positionMap = computed(() => new Map(positions.value.map((position) => [position.id, position])))
const volunteerNameMap = computed(() => new Map(volunteers.value.map((volunteer) => [
  volunteer.id,
  volunteer.realName || volunteer.nickname || volunteer.username || `志愿者 ${volunteer.id}`,
])))
const checkedInAssignmentIds = computed(() => new Set(checkins.value
  .filter((item) => item.checkinStatus === 'NORMAL' && item.checkinType === 'CHECK_IN')
  .map((item) => item.assignmentId)
  .filter(Boolean)))
const insights = computed<PositionInsight[]>(() => positions.value.map((position) => buildInsight(position)))
const selectedInsight = computed(() => insights.value.find((item) => item.position.id === selectedPositionId.value) || insights.value[0])
const highRiskCount = computed(() => insights.value.filter((item) => item.riskLevel === 'HIGH').length)
const mediumRiskCount = computed(() => insights.value.filter((item) => item.riskLevel === 'MEDIUM').length)
const totalRequired = computed(() => insights.value.reduce((sum, item) => sum + item.requiredCount, 0))
const totalAssigned = computed(() => insights.value.reduce((sum, item) => sum + item.assignedCount, 0))
const totalConfirmed = computed(() => insights.value.reduce((sum, item) => sum + item.confirmedCount, 0))
const totalCheckedIn = computed(() => insights.value.reduce((sum, item) => sum + item.checkedInCount, 0))
const pageCopy = computed(() => isShortagePage.value
  ? {
      title: '活动缺口分析',
      description: '按岗位自动汇总应排、已排、已确认和已签到人数，并可生成补位调度任务。',
      primaryAction: '检测并生成调度',
      emptyHint: '选择活动后会显示每个岗位的缺口预警。',
    }
  : {
      title: '调度任务工作台',
      description: '选择活动和岗位后，系统会带出缺口建议；高级条件可指定附近范围或候选人。',
      primaryAction: '执行补位调度',
      emptyHint: '选择活动和岗位后执行调度，结果会展示在这里。',
    })

watch(() => route.meta.module, () => {
  selectedTask.value = undefined
  tasks.value = []
})

onMounted(loadActivities)

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 100 })
    activities.value = Array.isArray(result) ? result : (result as PageResult<ActivityRecord>).records || []
    volunteers.value = await volunteerApi.list({ pageNo: 1, pageSize: 500 }).catch(() => [])
    if (!activityId.value && activities.value.length) {
      activityId.value = activities.value[0].id
    }
    await loadWorkbench()
  } finally {
    activityLoading.value = false
  }
}

async function loadWorkbench() {
  if (!activityId.value) {
    ElMessage.info('请先选择活动')
    return
  }
  loading.value = true
  try {
    const [areaResult, positionResult, detailResult, checkinResult] = await Promise.all([
      areaApi.list(activityId.value).catch(() => []),
      positionApi.list(activityId.value).catch(() => []),
      scheduleApi.detail(activityId.value, { silentError: true }).catch(() => undefined),
      locationApi.checkins({ activityId: activityId.value }).catch(() => []),
    ])
    areas.value = areaResult
    positions.value = positionResult
    detail.value = detailResult
    checkins.value = checkinResult
    if (!selectedPositionId.value || !positions.value.some((item) => item.id === selectedPositionId.value)) {
      selectedPositionId.value = positions.value[0]?.id
    }
  } finally {
    loading.value = false
  }
}

async function executeSelectedDispatch() {
  if (!activityId.value || !selectedInsight.value) {
    ElMessage.warning('请先选择活动和岗位')
    return
  }
  const shortage = resolveDispatchCount(selectedInsight.value)
  if (shortage <= 0) {
    ElMessage.success('当前岗位暂未发现需要补位的缺口')
    return
  }
  loading.value = true
  try {
    const task = await dispatchApi.execute({
      activityId: activityId.value,
      areaId: selectedInsight.value.position.areaId,
      positionId: selectedInsight.value.position.id,
      requiredCount: shortage,
      reason: advancedForm.reason || selectedInsight.value.actionText,
      createdBy: advancedForm.createdBy,
      longitude: advancedForm.longitude,
      latitude: advancedForm.latitude,
      radiusMeter: advancedForm.radiusMeter,
      candidateUserIds: parseCandidateUserIds(advancedForm.candidateUserIdsText),
    })
    pushTask(task)
    ElMessage.success('补位调度已生成')
  } finally {
    loading.value = false
  }
}

async function detectShortage() {
  if (!activityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  loading.value = true
  try {
    const result = await dispatchApi.detectShortage({
      activityId: activityId.value,
      longitude: advancedForm.longitude,
      latitude: advancedForm.latitude,
      radiusMeter: advancedForm.radiusMeter,
      createdBy: advancedForm.createdBy,
    })
    tasks.value = result
    selectedTask.value = result[0]
    ElMessage.success(result.length ? `已生成 ${result.length} 条补位调度` : '暂未发现签到缺口')
  } finally {
    loading.value = false
  }
}

async function queryResult() {
  if (!resultId.value) {
    ElMessage.warning('请输入调度任务编号')
    return
  }
  const task = await dispatchApi.result(resultId.value)
  pushTask(task)
}

function pushTask(task: DispatchRecord) {
  selectedTask.value = task
  tasks.value = [task, ...tasks.value.filter((item) => item.id !== task.id)]
}

function showTask(row: DispatchRecord) {
  selectedTask.value = row
}

function buildInsight(position: PositionRecord): PositionInsight {
  const positionAssignments = assignments.value.filter((item) => item.positionId === position.id)
  const requiredCount = position.needCount || 0
  const assignedCount = positionAssignments.length
  const confirmedCount = positionAssignments.filter((item) => item.assignmentStatus === 'CONFIRMED').length
  const checkedInCount = positionAssignments.filter((item) => item.id && checkedInAssignmentIds.value.has(item.id)).length
  const scheduleShortage = Math.max(requiredCount - assignedCount, 0)
  const confirmShortage = Math.max(assignedCount - confirmedCount, 0)
  const checkinShortage = Math.max(assignedCount - checkedInCount, 0)
  const suggestedShortage = scheduleShortage || checkinShortage || confirmShortage
  const riskLevel = scheduleShortage > 0 || checkinShortage > 0 ? 'HIGH' : confirmShortage > 0 ? 'MEDIUM' : 'LOW'
  return {
    position,
    areaName: position.areaId ? areaNameMap.value.get(position.areaId) || `区域 ${position.areaId}` : '-',
    requiredCount,
    assignedCount,
    confirmedCount,
    checkedInCount,
    scheduleShortage,
    confirmShortage,
    checkinShortage,
    suggestedShortage,
    riskLevel,
    riskText: riskLabel(riskLevel),
    actionText: scheduleShortage > 0 ? 'SCHEDULE_SHORTAGE' : checkinShortage > 0 ? 'CHECKIN_SHORTAGE' : 'CONFIRM_SHORTAGE',
  }
}

function resolveDispatchCount(insight: PositionInsight) {
  return advancedForm.requiredCount || insight.suggestedShortage || 0
}

function parseCandidateUserIds(text: string) {
  if (!text.trim()) {
    return undefined
  }
  return text.split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)
}

function riskLabel(level: PositionInsight['riskLevel']) {
  return level === 'HIGH' ? '高风险' : level === 'MEDIUM' ? '需关注' : '正常'
}

function riskTagType(level: PositionInsight['riskLevel']) {
  return level === 'HIGH' ? 'danger' : level === 'MEDIUM' ? 'warning' : 'success'
}

function statusTagType(status?: string) {
  if (status === 'FINISHED') return 'success'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'NO_CANDIDATE') return 'danger'
  return 'info'
}

function activityLabel(id?: number) {
  if (!id) return '-'
  return activityNameMap.value.get(id) || `活动 ${id}`
}

function areaLabel(id?: number) {
  if (!id) return '-'
  return areaNameMap.value.get(id) || `区域 ${id}`
}

function positionLabel(id?: number) {
  if (!id) return '-'
  const position = positionMap.value.get(id)
  return position?.name || `岗位 ${id}`
}

function volunteerLabel(id?: number) {
  if (!id) return '-'
  return volunteerNameMap.value.get(id) || `志愿者 ${id}`
}

function reasonLabel(reason?: string) {
  const labels: Record<string, string> = {
    MANUAL_DISPATCH: '手动调度',
    STAFF_SHORTAGE: '签到缺口补位',
    SCHEDULE_SHORTAGE: '排班缺口补位',
    CHECKIN_SHORTAGE: '签到缺口补位',
    CONFIRM_SHORTAGE: '确认缺口提醒',
  }
  return reason ? labels[reason] || reason : '-'
}

function dispatchStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    FINISHED: '已完成',
    PARTIAL: '部分推荐',
    NO_CANDIDATE: '无候选人',
    PENDING: '处理中',
  }
  return status ? labels[status] || status : '-'
}

function recommendStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    RECOMMENDED: '已推荐',
    ACCEPTED: '已接受',
    REJECTED: '已拒绝',
  }
  return status ? labels[status] || status : '-'
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
          @change="loadWorkbench"
        >
          <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
        </el-select>
        <el-select v-model="selectedPositionId" filterable placeholder="选择岗位" style="width: 240px">
          <el-option v-for="item in insights" :key="item.position.id" :label="`${item.position.name}（${item.position.id}）`" :value="item.position.id" />
        </el-select>
        <el-button :icon="Refresh" @click="loadWorkbench">刷新分析</el-button>
      </div>
      <div class="command-row">
        <el-button :icon="Connection" @click="router.push('/schedule/plans')">排班计划</el-button>
        <el-button
          :type="isShortagePage ? 'warning' : 'primary'"
          :icon="isShortagePage ? Warning : Aim"
          :loading="loading"
          @click="isShortagePage ? detectShortage() : executeSelectedDispatch()"
        >
          {{ pageCopy.primaryAction }}
        </el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="metric-card">
        <span>应排/已排</span>
        <strong>{{ totalRequired }} / {{ totalAssigned }}</strong>
        <small>{{ selectedActivity?.name || '未选择活动' }}</small>
      </div>
      <div class="metric-card">
        <span>已确认</span>
        <strong>{{ totalConfirmed }}</strong>
        <small>未确认 {{ Math.max(totalAssigned - totalConfirmed, 0) }} 人</small>
      </div>
      <div class="metric-card">
        <span>已签到</span>
        <strong>{{ totalCheckedIn }}</strong>
        <small>签到状态来自 checkin_record</small>
      </div>
      <div class="metric-card">
        <span>风险岗位</span>
        <strong>{{ highRiskCount }}</strong>
        <small>另有 {{ mediumRiskCount }} 个岗位需关注</small>
      </div>
    </div>

    <div class="table-panel dispatch-context">
      <div class="table-meta">
        <span>{{ pageCopy.title }}</span>
        <small>{{ pageCopy.description }}</small>
      </div>
      <el-table v-loading="loading" :data="insights" row-key="position.id" highlight-current-row @current-change="(row: PositionInsight) => selectedPositionId = row?.position.id">
        <el-table-column label="岗位" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ row.position.name }}</strong>
              <small>{{ row.areaName }} · 岗位 {{ row.position.id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="requiredCount" label="应排" width="82" />
        <el-table-column prop="assignedCount" label="已排" width="82" />
        <el-table-column prop="confirmedCount" label="已确认" width="92" />
        <el-table-column prop="checkedInCount" label="已签到" width="92" />
        <el-table-column label="缺口" min-width="180">
          <template #default="{ row }">
            排班 {{ row.scheduleShortage }} / 确认 {{ row.confirmShortage }} / 签到 {{ row.checkinShortage }}
          </template>
        </el-table-column>
        <el-table-column label="风险" width="110">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="light">{{ row.riskText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.suggestedShortage" @click="selectedPositionId = row.position.id; executeSelectedDispatch()">生成调度</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !insights.length" :description="pageCopy.emptyHint" />
    </div>

    <div class="table-panel advanced-panel">
      <div class="table-meta">
        <span>调度条件</span>
        <small>默认按当前岗位缺口生成；需要指定附近范围或候选人时再展开。</small>
      </div>
      <el-button text type="primary" @click="advancedVisible = !advancedVisible">{{ advancedVisible ? '收起高级条件' : '展开高级条件' }}</el-button>
      <el-form v-if="advancedVisible" :model="advancedForm" label-width="104px" class="inline-form">
        <el-form-item label="覆盖缺口数">
          <el-input-number v-model="advancedForm.requiredCount" :min="1" clearable />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="advancedForm.reason" placeholder="默认按缺口类型自动填写" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="advancedForm.longitude" :precision="6" :controls="false" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="advancedForm.latitude" :precision="6" :controls="false" />
        </el-form-item>
        <el-form-item label="半径米">
          <el-input-number v-model="advancedForm.radiusMeter" :min="1" />
        </el-form-item>
        <el-form-item label="候选人">
          <el-input v-model="advancedForm.candidateUserIdsText" placeholder="可选，多个志愿者编号用英文逗号分隔" />
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar result-toolbar">
      <div class="filter-row">
        <el-input-number v-model="resultId" :min="1" :controls="false" placeholder="调度任务编号" />
        <el-button :icon="Search" @click="queryResult">查询结果</el-button>
      </div>
      <div class="command-row">
        <el-button :icon="Refresh" :disabled="!selectedTask?.id" @click="selectedTask?.id && (resultId = selectedTask.id, queryResult())">刷新当前</el-button>
      </div>
    </div>

    <div class="table-panel detail-panel">
      <div class="table-meta">
        <span>调度任务结果</span>
        <small>共 {{ tasks.length }} 条，点击任务可查看推荐人员。</small>
      </div>
      <el-table v-loading="loading" :data="tasks" row-key="id" highlight-current-row @current-change="showTask">
        <el-table-column prop="id" label="任务编号" width="110" />
        <el-table-column label="活动" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ activityLabel(row.activityId) }}</strong>
              <small>活动 {{ row.activityId || '-' }} · 创建人 {{ row.createdBy || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="区域" min-width="150">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ areaLabel(row.areaId) }}</strong>
              <small>区域 {{ row.areaId || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="岗位" min-width="160">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ positionLabel(row.positionId) }}</strong>
              <small>岗位 {{ row.positionId || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="requiredCount" label="缺口" width="82" />
        <el-table-column label="原因" min-width="140">
          <template #default="{ row }">{{ reasonLabel(row.reason) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.dispatchStatus)" effect="light">{{ dispatchStatusLabel(row.dispatchStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="160">
          <template #default="{ row }">{{ fmt(row.finishedTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !tasks.length" description="还没有调度任务结果" />
    </div>

    <div class="table-panel detail-panel">
      <div class="table-meta">
        <span>推荐人员</span>
        <small>任务 {{ selectedTask?.id || '-' }}，共 {{ recommendations.length }} 人</small>
      </div>
      <el-table :data="recommendations" row-key="id">
        <el-table-column label="志愿者" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ volunteerLabel(row.userId) }}</strong>
              <small>志愿者 {{ row.userId || '-' }} · 推荐 {{ row.id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="distanceMeter" label="距离米" width="120" />
        <el-table-column prop="matchScore" label="匹配分" width="120" />
        <el-table-column label="推荐状态" min-width="130">
          <template #default="{ row }">{{ recommendStatusLabel(row.recommendStatus) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.dispatch-context,
.advanced-panel,
.detail-panel,
.result-toolbar {
  margin-top: 18px;
}

.inline-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  column-gap: 20px;
  margin-top: 8px;
}
</style>
