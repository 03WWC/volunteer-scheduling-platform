<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowUp, Key, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { activityApi, locationApi, positionApi, scheduleApi, volunteerApi } from '@/api/modules'
import type {
  ActivityRecord,
  CheckinQrCodeRecord,
  CheckinRecord,
  PageResult,
  PositionRecord,
  ScheduleAssignmentRecord,
  VolunteerRecord,
} from '@/types/api'

interface StoredCheckinQrCodeRecord extends CheckinQrCodeRecord {
  generatedAt: string
}

interface PositionCheckinRow extends PositionRecord {
  assignedCount: number
  checkedInCount: number
  checkedOutCount: number
  pendingCount: number
  startTime?: string
  endTime?: string
}

const QR_HISTORY_KEY = 'checkin-qrcode-history'
const loading = ref(false)
const activityLoading = ref(false)
const activityId = ref<number>()
const positionKeyword = ref('')
const activities = ref<ActivityRecord[]>([])
const positions = ref<PositionRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const assignments = ref<ScheduleAssignmentRecord[]>([])
const checkins = ref<CheckinRecord[]>([])
const qrCode = ref<CheckinQrCodeRecord>()
const qrHistory = ref<StoredCheckinQrCodeRecord[]>([])
const positionNameMap = computed(() => new Map(positions.value.map((item) => [item.id, item.name])))
const volunteerNameMap = computed(() => new Map(volunteers.value.map((item) => [
  item.id,
  item.realName || item.nickname || item.username || `志愿者 ${item.id}`,
])))
const assignmentsByPosition = computed(() => {
  const map = new Map<number, ScheduleAssignmentRecord[]>()
  assignments.value.forEach((item) => {
    if (!item.positionId) {
      return
    }
    const items = map.get(item.positionId) || []
    items.push(item)
    map.set(item.positionId, items)
  })
  return map
})
const positionRows = computed<PositionCheckinRow[]>(() => positions.value.map((position) => {
  const positionAssignments = assignmentsByPosition.value.get(position.id) || []
  const assignmentIds = new Set(positionAssignments.map((item) => item.id))
  const checkedInIds = new Set(checkins.value
    .filter((item) => item.checkinType === 'CHECK_IN' && item.checkinStatus === 'NORMAL' && assignmentIds.has(item.assignmentId))
    .map((item) => item.assignmentId))
  const checkedOutIds = new Set(checkins.value
    .filter((item) => item.checkinType === 'CHECK_OUT' && item.checkinStatus === 'NORMAL' && assignmentIds.has(item.assignmentId))
    .map((item) => item.assignmentId))
  const startTimes = positionAssignments.map((item) => item.startTime).filter(Boolean) as string[]
  const endTimes = positionAssignments.map((item) => item.endTime).filter(Boolean) as string[]
  const sortedStartTimes = startTimes.sort()
  const sortedEndTimes = endTimes.sort()
  return {
    ...position,
    assignedCount: positionAssignments.length,
    checkedInCount: checkedInIds.size,
    checkedOutCount: checkedOutIds.size,
    pendingCount: Math.max(positionAssignments.length - checkedInIds.size, 0),
    startTime: sortedStartTimes[0] || position.startTime,
    endTime: sortedEndTimes[sortedEndTimes.length - 1] || position.endTime,
  }
}))
const visiblePositionRows = computed(() => positionRows.value.filter((item) => {
  const keyword = positionKeyword.value.trim()
  if (!keyword) {
    return true
  }
  const text = `${item.id} ${item.name} ${positionLabel(item.id)} ${fmt(item.startTime)} ${fmt(item.endTime)}`
  return text.includes(keyword)
}))
const visibleQrHistory = computed(() => qrHistory.value
  .filter((item) => !activityId.value || item.activityId === activityId.value)
  .slice(0, 20))
const qrImageUrl = computed(() => {
  if (!qrCode.value?.qrCode) {
    return ''
  }
  return qrCode.value.qrCode
})

onMounted(async () => {
  loadQrHistory()
  await loadActivities()
})

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 100 })
    activities.value = Array.isArray(result) ? result : (result as PageResult<ActivityRecord>).records || []
    volunteers.value = await volunteerApi.list({ pageNo: 1, pageSize: 500 }).catch(() => [])
    if (!activityId.value && activities.value.length) {
      activityId.value = activities.value[0].id
      await loadData()
    }
  } finally {
    activityLoading.value = false
  }
}

async function loadData() {
  if (!activityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  loading.value = true
  try {
    const [detail, positionResult, checkinResult] = await Promise.all([
      scheduleApi.detail(activityId.value),
      positionApi.list(activityId.value).catch(() => []),
      locationApi.checkins({ activityId: activityId.value }),
    ])
    assignments.value = detail.assignments || []
    positions.value = positionResult
    checkins.value = checkinResult
  } finally {
    loading.value = false
  }
}

async function generateQrCode(row: PositionCheckinRow, type: string) {
  if (!row || !activityId.value) {
    ElMessage.warning('请先选择活动和岗位')
    return
  }
  qrCode.value = await locationApi.createQrCode({
    activityId: activityId.value,
    positionId: row.id,
    checkinType: type,
  })
  saveQrHistory(qrCode.value)
  ElMessage.success(`${checkinTypeLabel(type)}码已生成`)
}

function showQrCode(row: StoredCheckinQrCodeRecord) {
  qrCode.value = row
}

function hideQrCode() {
  qrCode.value = undefined
}

function loadQrHistory() {
  try {
    const raw = localStorage.getItem(QR_HISTORY_KEY)
    qrHistory.value = raw ? JSON.parse(raw) : []
  } catch {
    qrHistory.value = []
  }
}

function saveQrHistory(record: CheckinQrCodeRecord) {
  const storedRecord: StoredCheckinQrCodeRecord = {
    ...record,
    generatedAt: new Date().toISOString(),
  }
  qrHistory.value = [
    storedRecord,
    ...qrHistory.value.filter((item) => !(item.activityId === record.activityId && item.positionId === record.positionId && item.checkinType === record.checkinType)),
  ].slice(0, 50)
  localStorage.setItem(QR_HISTORY_KEY, JSON.stringify(qrHistory.value))
}

function positionLabel(positionId?: number) {
  if (!positionId) {
    return '未指定岗位'
  }
  return `${positionNameMap.value.get(positionId) || '岗位'}（${positionId}）`
}

function volunteerLabel(userId?: number) {
  if (!userId) {
    return '未指定志愿者'
  }
  return `${volunteerNameMap.value.get(userId) || '志愿者'}（${userId}）`
}

function checkinTypeLabel(type?: string) {
  return type === 'CHECK_OUT' ? '签退' : '签到'
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    NORMAL: '正常',
    PENDING: '处理中',
    ABNORMAL: '异常',
  }
  return status ? labels[status] || status : '-'
}

function qrHistoryMeta(row: StoredCheckinQrCodeRecord) {
  return `通用岗位码 · 生成 ${fmt(row.generatedAt)} · 有效至 ${fmt(row.expireTime)}`
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
          @change="loadData"
        >
          <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
        </el-select>
        <el-button :icon="Search" @click="loadData">查询签到</el-button>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <div class="table-panel checkin-generator">
      <div class="table-meta">
        <span>生成岗位通用码</span>
        <small>同一岗位共用一张签到或签退码；志愿者扫码后会按当前账号自动匹配自己的排班。</small>
      </div>
      <div class="assignment-tools">
        <el-input v-model="positionKeyword" clearable :prefix-icon="Search" placeholder="搜索岗位名称或岗位编号" />
      </div>
      <el-table v-loading="loading" :data="visiblePositionRows" row-key="id">
        <el-table-column label="岗位" min-width="240">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ row.name }}</strong>
              <span>岗位 {{ row.id }} · 需求 {{ row.needCount || 0 }} 人</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="服务时间" min-width="230">
          <template #default="{ row }">{{ fmt(row.startTime) }} - {{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="现场进度" min-width="230">
          <template #default="{ row }">
            <div class="checkin-progress">
              <el-tag effect="light">已排 {{ row.assignedCount }}</el-tag>
              <el-tag type="warning" effect="light">未签 {{ row.pendingCount }}</el-tag>
              <el-tag type="success" effect="light">已签 {{ row.checkedInCount }}</el-tag>
              <el-tag type="info" effect="light">签退 {{ row.checkedOutCount }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Key" @click="generateQrCode(row, 'CHECK_IN')">签到码</el-button>
            <el-button link type="success" :icon="Key" @click="generateQrCode(row, 'CHECK_OUT')">签退码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !visiblePositionRows.length" description="暂无符合条件的岗位" />
    </div>

    <div v-if="qrCode" class="table-panel qr-panel">
      <div class="table-meta">
        <span>{{ positionLabel(qrCode.positionId) }} · 通用{{ checkinTypeLabel(qrCode.checkinType) }}码</span>
        <small>有效期至 {{ fmt(qrCode.expireTime) }}，该岗位志愿者扫码后自动匹配排班</small>
      </div>
      <div class="qr-preview">
        <img v-if="qrImageUrl" class="qr-image" :src="qrImageUrl" alt="签到二维码" />
      </div>
      <div class="qr-actions">
        <el-button :icon="ArrowUp" @click="hideQrCode">收起二维码</el-button>
      </div>
    </div>

    <div class="table-panel qr-history-panel">
      <div class="table-meta">
        <span>最近生成签到码</span>
        <small>生成签到码不会写入签到记录；扫码成功后才会出现在下方签到记录中。</small>
      </div>
      <el-table :data="visibleQrHistory" row-key="qrCodeToken">
        <el-table-column label="签到码" min-width="360">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ positionLabel(row.positionId) }} · {{ checkinTypeLabel(row.checkinType) }}码</strong>
              <span>{{ qrHistoryMeta(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showQrCode(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!visibleQrHistory.length" description="暂无生成过的签到码" />
    </div>

    <div class="table-panel">
      <div class="table-meta">
        <span>共 {{ checkins.length }} 条签到记录</span>
        <small>扫码签到后实时写入</small>
      </div>
      <el-table v-loading="loading" :data="checkins" row-key="id">
        <el-table-column prop="id" label="记录编号" width="110" />
        <el-table-column prop="assignmentId" label="安排编号" width="110" />
        <el-table-column label="岗位" min-width="180">
          <template #default="{ row }">{{ positionLabel(row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="志愿者" min-width="180">
          <template #default="{ row }">{{ volunteerLabel(row.userId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="95">
          <template #default="{ row }">{{ checkinTypeLabel(row.checkinType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ statusLabel(row.checkinStatus) }}</template>
        </el-table-column>
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.checkinTime) }}</template>
        </el-table-column>
        <el-table-column label="GPS" min-width="190">
          <template #default="{ row }">{{ row.longitude || '-' }}, {{ row.latitude || '-' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.checkin-generator {
  margin-bottom: 18px;
}

.assignment-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #edf1ef;
}

.assignment-tools .el-input {
  width: 320px;
}

.assignment-tools .el-select {
  width: 240px;
}

.checkin-progress {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.qr-panel {
  margin-bottom: 18px;
}

.qr-history-panel {
  margin-bottom: 18px;
}

.qr-preview {
  display: flex;
  justify-content: center;
  padding: 18px 16px 6px;
}

.qr-image {
  width: 220px;
  height: 220px;
  border: 1px solid #e1e8e5;
  border-radius: 8px;
  background: #fff;
  object-fit: contain;
}

.qr-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 720px) {
  .assignment-tools {
    align-items: stretch;
    flex-direction: column;
  }

  .assignment-tools .el-input,
  .assignment-tools .el-select {
    width: 100%;
  }

  .qr-image {
    width: 190px;
    height: 190px;
  }
}
</style>
