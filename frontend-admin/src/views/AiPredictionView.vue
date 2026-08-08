<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { DataAnalysis, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { activityApi, aiApi, areaApi, locationApi, positionApi, scheduleApi, volunteerApi } from '@/api/modules'
import type {
  ActivityRecord,
  ActivitySignupRecord,
  AiAreaRiskInput,
  AiPredictionResult,
  AiPositionRiskInput,
  AiVolunteerRiskInput,
  AreaRecord,
  CheckinRecord,
  PageResult,
  PositionRecord,
  ScheduleAssignmentRecord,
  VolunteerRecord,
} from '@/types/api'

const loading = ref(false)
const activityLoading = ref(false)
const activityId = ref<number>()
const activities = ref<ActivityRecord[]>([])
const areas = ref<AreaRecord[]>([])
const positions = ref<PositionRecord[]>([])
const assignments = ref<ScheduleAssignmentRecord[]>([])
const checkins = ref<CheckinRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const signups = ref<ActivitySignupRecord[]>([])
const prediction = ref<AiPredictionResult>()

const selectedActivity = computed(() => activities.value.find((item) => item.id === activityId.value))
const summary = computed(() => [
  { label: '缺岗风险', value: prediction.value?.shortageRisks?.filter((item) => item.riskLevel === 'HIGH').length || 0 },
  { label: '流失风险', value: prediction.value?.attritionRisks?.filter((item) => item.riskLevel === 'HIGH').length || 0 },
  { label: '高风险区域', value: prediction.value?.areaRisks?.filter((item) => item.riskLevel === 'HIGH').length || 0 },
])

onMounted(loadActivities)

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 100 })
    activities.value = Array.isArray(result) ? result : (result as PageResult<ActivityRecord>).records || []
    if (!activityId.value && activities.value.length) {
      activityId.value = activities.value[0].id
      await runPrediction()
    }
  } finally {
    activityLoading.value = false
  }
}

async function runPrediction() {
  if (!activityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  loading.value = true
  try {
    await loadSourceData(activityId.value)
    prediction.value = await aiApi.predictRisks({
      activityId: activityId.value,
      positions: buildPositionInputs(),
      volunteers: buildVolunteerInputs(),
      areas: buildAreaInputs(),
    })
    ElMessage.success('AI 风险预测已刷新')
  } finally {
    loading.value = false
  }
}

async function loadSourceData(id: number) {
  const [areaRows, positionRows, scheduleDetail, checkinRows, volunteerRows, signupRows] = await Promise.all([
    areaApi.list(id).catch(() => []),
    positionApi.list(id).catch(() => []),
    scheduleApi.detail(id, { silentError: true }).catch(() => ({ assignments: [] })),
    locationApi.checkins({ activityId: id }).catch(() => []),
    volunteerApi.list({ pageNo: 1, pageSize: 500 }).catch(() => []),
    activityApi.signups({ activityId: id }).catch(() => []),
  ])
  areas.value = areaRows
  positions.value = positionRows
  assignments.value = scheduleDetail.assignments || []
  checkins.value = checkinRows
  volunteers.value = volunteerRows
  signups.value = signupRows
}

function buildPositionInputs(): AiPositionRiskInput[] {
  return positions.value.map((position) => {
    const positionAssignments = assignments.value.filter((item) => item.positionId === position.id)
    return {
      positionId: position.id,
      areaId: position.areaId,
      positionName: position.name,
      requiredCount: Number(position.needCount || 0),
      assignedCount: positionAssignments.length,
      checkedInCount: countCheckedIn(positionAssignments),
    }
  })
}

function buildVolunteerInputs(): AiVolunteerRiskInput[] {
  const scheduledUserIds = new Set(assignments.value.map((item) => item.userId).filter(Boolean) as number[])
  return volunteers.value
    .filter((volunteer) => scheduledUserIds.has(volunteer.id) || signups.value.some((signup) => signup.userId === volunteer.id))
    .map((volunteer) => {
      const userAssignments = assignments.value.filter((item) => item.userId === volunteer.id)
      const userCheckins = checkins.value.filter((item) => item.userId === volunteer.id && item.checkinType === 'CHECK_IN')
      const cancelledCount = signups.value.filter((item) => item.userId === volunteer.id && isCancelled(item.signupStatus)).length
      const checkinTimes = userCheckins.map((item) => item.checkinTime).filter(Boolean).sort()
      const lastCheckinTime = checkinTimes[checkinTimes.length - 1]
      return {
        userId: volunteer.id,
        userName: volunteer.realName || volunteer.nickname || volunteer.username || `志愿者 ${volunteer.id}`,
        assignedCount: userAssignments.length,
        checkedInCount: new Set(userCheckins.map((item) => item.assignmentId)).size,
        cancelledCount,
        lastActiveTime: lastCheckinTime,
      }
    })
}

function buildAreaInputs(): AiAreaRiskInput[] {
  return areas.value.map((area) => {
    const areaPositions = positions.value.filter((item) => item.areaId === area.id)
    const areaAssignments = assignments.value.filter((item) => item.areaId === area.id || areaPositions.some((position) => position.id === item.positionId))
    return {
      areaId: area.id,
      areaName: area.name,
      requiredCount: areaPositions.reduce((sum, item) => sum + Number(item.needCount || 0), 0),
      assignedCount: areaAssignments.length,
      checkedInCount: countCheckedIn(areaAssignments),
    }
  })
}

function countCheckedIn(rows: ScheduleAssignmentRecord[]) {
  const assignmentIds = new Set(rows.map((item) => item.id))
  return new Set(checkins.value
    .filter((item) => item.checkinType === 'CHECK_IN' && assignmentIds.has(item.assignmentId))
    .map((item) => item.assignmentId)).size
}

function isCancelled(status?: string) {
  return ['CANCELLED', 'REJECTED', 'CANCEL'].includes(String(status || '').toUpperCase())
}

function riskType(level?: string): 'success' | 'warning' | 'danger' {
  return level === 'HIGH' ? 'danger' : level === 'MEDIUM' ? 'warning' : 'success'
}

function riskLabel(level?: string) {
  return level === 'HIGH' ? '高风险' : level === 'MEDIUM' ? '中风险' : '低风险'
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
          style="width: 300px"
          @change="runPrediction"
        >
          <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
        </el-select>
        <el-button :icon="Search" :loading="loading" @click="runPrediction">生成预测</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="runPrediction">刷新</el-button>
      </div>
    </div>

    <div class="risk-summary">
      <article v-for="item in summary" :key="item.label" class="risk-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
      <article class="risk-card muted">
        <span>当前活动</span>
        <strong>{{ selectedActivity?.name || '-' }}</strong>
      </article>
    </div>

    <div class="table-panel">
      <div class="table-meta">
        <span>缺岗风险预测</span>
        <small>按岗位需求、排班覆盖和签到覆盖计算</small>
      </div>
      <el-table v-loading="loading" :data="prediction?.shortageRisks || []" row-key="positionId">
        <el-table-column prop="positionId" label="岗位编号" width="110" />
        <el-table-column prop="positionName" label="岗位" min-width="150" />
        <el-table-column prop="shortageCount" label="预测缺口" width="110" />
        <el-table-column prop="riskScore" label="风险分" width="100" />
        <el-table-column label="等级" width="110">
          <template #default="{ row }"><el-tag :type="riskType(row.riskLevel)">{{ riskLabel(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="280" />
      </el-table>
    </div>

    <div class="table-panel detail-panel">
      <div class="table-meta">
        <span>人员流失风险</span>
        <small>按缺勤、取消和最近活跃综合计算</small>
      </div>
      <el-table v-loading="loading" :data="prediction?.attritionRisks || []" row-key="userId">
        <el-table-column prop="userId" label="志愿者编号" width="125" />
        <el-table-column prop="userName" label="志愿者" min-width="150" />
        <el-table-column prop="riskScore" label="风险分" width="100" />
        <el-table-column label="等级" width="110">
          <template #default="{ row }"><el-tag :type="riskType(row.riskLevel)">{{ riskLabel(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="300" />
      </el-table>
    </div>

    <div class="table-panel detail-panel">
      <div class="table-meta">
        <span>高风险区域识别</span>
        <small>按区域岗位缺口和签到损耗计算</small>
      </div>
      <el-table v-loading="loading" :data="prediction?.areaRisks || []" row-key="areaId">
        <el-table-column prop="areaId" label="区域编号" width="110" />
        <el-table-column prop="areaName" label="区域" min-width="150" />
        <el-table-column prop="shortageCount" label="区域缺口" width="110" />
        <el-table-column prop="riskScore" label="风险分" width="100" />
        <el-table-column label="等级" width="110">
          <template #default="{ row }"><el-tag :type="riskType(row.riskLevel)">{{ riskLabel(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="300" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.risk-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.risk-card {
  min-height: 92px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.risk-card span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.risk-card strong {
  display: block;
  margin-top: 10px;
  color: #1e332d;
  font-size: 28px;
}

.risk-card.muted strong {
  font-size: 18px;
}

.detail-panel {
  margin-top: 18px;
}

@media (max-width: 980px) {
  .risk-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
