<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Close, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, positionApi, volunteerApi } from '@/api/modules'
import type { ActivityRecord, ActivitySignupRecord, PageResult, PositionRecord, VolunteerRecord } from '@/types/api'

const loading = ref(false)
const keyword = ref('')
const filters = reactive({ status: 'PENDING', activityId: undefined as number | undefined })
const signups = ref<ActivitySignupRecord[]>([])
const activities = ref<ActivityRecord[]>([])
const positions = ref<PositionRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])

const activityNameMap = computed(() => new Map(activities.value.map((item) => [item.id, item.name])))
const positionNameMap = computed(() => new Map(positions.value.map((item) => [item.id, item.name])))
const volunteerNameMap = computed(() => new Map(volunteers.value.map((item) => [item.id, item.realName || item.username || item.nickname || `志愿者 ${item.id}`])))

const statusText: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
}

const visibleSignups = computed(() =>
  signups.value.filter((item) => {
    const text = `${item.id} ${item.activityId} ${item.userId} ${item.positionId || ''}`
    const names = `${activityName(item.activityId)} ${positionName(item.positionId)} ${volunteerName(item.userId)}`
    return !keyword.value || `${text} ${names}`.includes(keyword.value)
  }),
)

onMounted(async () => {
  await loadReferences()
  await loadSignups()
})

async function loadReferences() {
  const [activityResult, volunteerResult] = await Promise.all([
    activityApi.page({ pageNo: 1, pageSize: 100 }),
    volunteerApi.list({}),
  ])
  activities.value = Array.isArray(activityResult) ? activityResult : (activityResult as PageResult<ActivityRecord>).records || []
  volunteers.value = volunteerResult
}

async function loadSignups() {
  loading.value = true
  try {
    signups.value = await activityApi.signups({
      activityId: filters.activityId || undefined,
      signupStatus: filters.status || undefined,
    })
    await loadPositionsForSignups()
  } finally {
    loading.value = false
  }
}

async function loadPositionsForSignups() {
  const activityIds = Array.from(new Set(signups.value.map((item) => item.activityId).filter(Boolean)))
  const list = await Promise.all(activityIds.map((activityId) => positionApi.list(activityId)))
  positions.value = list.flat()
}

async function approve(row: ActivitySignupRecord) {
  await activityApi.approveSignup(row.id)
  ElMessage.success('已通过报名')
  await loadSignups()
}

async function reject(row: ActivitySignupRecord) {
  await ElMessageBox.confirm('拒绝后志愿者将不能进入本次活动排班，是否继续？', '拒绝报名', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await activityApi.rejectSignup(row.id)
  ElMessage.success('已拒绝报名')
  await loadSignups()
}

function activityName(activityId: number) {
  return activityNameMap.value.get(activityId) || `活动 ${activityId}`
}

function positionName(positionId?: number) {
  return positionId ? positionNameMap.value.get(positionId) || `岗位 ${positionId}` : '未指定岗位'
}

function volunteerName(userId: number) {
  return volunteerNameMap.value.get(userId) || `志愿者 ${userId}`
}
</script>

<template>
  <section class="signup-review">
    <div class="toolbar">
      <div class="filter-row">
        <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索报名编号、活动编号、志愿者编号" />
        <el-select v-model="filters.activityId" filterable clearable placeholder="全部活动" @change="loadSignups">
          <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态" @change="loadSignups">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button :icon="Refresh" @click="loadSignups">刷新</el-button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-meta"><span>共 {{ visibleSignups.length }} 条报名记录</span><small>用于报名审核和排班准入</small></div>
      <el-table v-loading="loading" :data="visibleSignups" row-key="id">
        <el-table-column prop="id" label="报名编号" width="110" />
        <el-table-column label="活动" min-width="180">
          <template #default="{ row }">{{ activityName(row.activityId) }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="150">
          <template #default="{ row }">{{ positionName(row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="志愿者" min-width="150">
          <template #default="{ row }">{{ volunteerName(row.userId) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="报名备注" min-width="180">
          <template #default="{ row }">{{ row.remark || '无' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="row.signupStatus === 'PENDING' ? 'warning' : row.signupStatus === 'APPROVED' ? 'success' : 'info'" effect="light">
              {{ statusText[row.signupStatus] || row.signupStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.signupStatus === 'PENDING'" link type="primary" :icon="Check" @click="approve(row)">通过</el-button>
            <el-button v-if="row.signupStatus === 'PENDING'" link type="danger" :icon="Close" @click="reject(row)">拒绝</el-button>
            <span v-else class="muted-action">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.signup-review { display: flex; flex-direction: column; gap: 18px; }
.muted-action { color: #95a19d; font-size: 13px; }
</style>
