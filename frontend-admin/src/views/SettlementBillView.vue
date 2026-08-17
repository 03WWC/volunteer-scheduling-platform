<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Money, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { activityApi, positionApi, scheduleApi, settlementApi, volunteerApi } from '@/api/modules'
import type {
  ActivityRecord,
  PageResult,
  PositionRecord,
  ScheduleAssignmentRecord,
  SettlementBillRecord,
  VolunteerRecord,
} from '@/types/api'

const loading = ref(false)
const userId = ref<number>()
const selectedBill = ref<SettlementBillRecord>()
const bills = ref<SettlementBillRecord[]>([])
const catalogLoading = ref(false)
const activities = ref<ActivityRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const positions = ref<PositionRecord[]>([])
const assignments = ref<ScheduleAssignmentRecord[]>([])
const generateDialogVisible = ref(false)
const payDialogVisible = ref(false)
const generateFormRef = ref<FormInstance>()
const payFormRef = ref<FormInstance>()
const generateForm = reactive({
  activityId: undefined as number | undefined,
  userId: undefined as number | undefined,
  baseAmount: 0,
  hourlyRate: 30,
  rewardAmount: 0,
  deductAmount: 0,
})
const payForm = reactive({
  billId: undefined as number | undefined,
  payChannel: 'BANK',
  payAmount: 0,
})

const details = computed(() => selectedBill.value?.details || [])
const payments = computed(() => selectedBill.value?.payments || [])
const activityNameMap = computed(() => new Map(activities.value.map((item) => [item.id, item.name])))
const volunteerNameMap = computed(() => new Map(volunteers.value.map((item) => [
  item.id,
  item.realName || item.nickname || item.username || `志愿者 ${item.id}`,
])))
const positionNameMap = computed(() => new Map(positions.value.map((item) => [item.id, item.name])))
const assignmentMap = computed(() => new Map(assignments.value.map((item) => [item.id, item])))

onMounted(loadCatalogs)

async function loadCatalogs() {
  catalogLoading.value = true
  try {
    const [activityResult, volunteerResult] = await Promise.all([
      activityApi.page({ pageNo: 1, pageSize: 200 }),
      volunteerApi.list({ pageNo: 1, pageSize: 500 }),
    ])
    activities.value = Array.isArray(activityResult) ? activityResult : (activityResult as PageResult<ActivityRecord>).records || []
    volunteers.value = volunteerResult
  } finally {
    catalogLoading.value = false
  }
}

async function loadBills() {
  if (!userId.value) {
    ElMessage.warning('请选择志愿者')
    return
  }
  loading.value = true
  try {
    bills.value = await settlementApi.listByUser(userId.value)
    selectedBill.value = bills.value[0]
    if (selectedBill.value?.activityId) {
      await loadActivityContext(selectedBill.value.activityId)
    }
  } finally {
    loading.value = false
  }
}

function openGenerate() {
  Object.assign(generateForm, {
    activityId: undefined,
    userId: userId.value,
    baseAmount: 0,
    hourlyRate: 30,
    rewardAmount: 0,
    deductAmount: 0,
  })
  generateDialogVisible.value = true
}

async function loadActivityContext(activityId?: number) {
  if (!activityId) {
    positions.value = []
    assignments.value = []
    return
  }
  const [positionResult, scheduleDetail] = await Promise.all([
    positionApi.list(activityId).catch(() => []),
    scheduleApi.detail(activityId, { silentError: true }).catch(() => undefined),
  ])
  positions.value = positionResult
  assignments.value = scheduleDetail?.assignments || []
}

async function generateBill() {
  await generateFormRef.value?.validate()
  selectedBill.value = await settlementApi.generate({
    activityId: generateForm.activityId,
    userId: generateForm.userId,
    baseAmount: generateForm.baseAmount,
    hourlyRate: generateForm.hourlyRate,
    rewardAmount: generateForm.rewardAmount,
    deductAmount: generateForm.deductAmount,
  })
  userId.value = generateForm.userId
  generateDialogVisible.value = false
  ElMessage.success('结算单已生成')
  await loadBills()
}

async function confirmBill(row: SettlementBillRecord) {
  selectedBill.value = await settlementApi.confirm(row.id)
  ElMessage.success('结算单已确认')
  await loadBills()
}

function openPay(row: SettlementBillRecord) {
  Object.assign(payForm, {
    billId: row.id,
    payChannel: 'BANK',
    payAmount: row.totalAmount,
  })
  payDialogVisible.value = true
}

async function payBill() {
  await payFormRef.value?.validate()
  if (!payForm.billId) {
    return
  }
  selectedBill.value = await settlementApi.pay(payForm.billId, {
    payChannel: payForm.payChannel,
    payAmount: payForm.payAmount,
  })
  payDialogVisible.value = false
  ElMessage.success('付款已登记')
  await loadBills()
}

async function showDetail(row: SettlementBillRecord) {
  selectedBill.value = await settlementApi.detail(row.id)
  await loadActivityContext(row.activityId)
}

function activityLabel(activityId?: number) {
  if (!activityId) {
    return '未指定活动'
  }
  return activityNameMap.value.get(activityId) || `活动 ${activityId}`
}

function volunteerLabel(targetUserId?: number) {
  if (!targetUserId) {
    return '未指定志愿者'
  }
  return volunteerNameMap.value.get(targetUserId) || `志愿者 ${targetUserId}`
}

function positionLabel(positionId?: number) {
  if (!positionId) {
    return '未指定岗位'
  }
  return positionNameMap.value.get(positionId) || `岗位 ${positionId}`
}

function assignmentLabel(assignmentId?: number, positionId?: number) {
  const assignment = assignmentId ? assignmentMap.value.get(assignmentId) : undefined
  const userName = volunteerLabel(assignment?.userId || selectedBill.value?.userId)
  const positionName = positionLabel(assignment?.positionId || positionId)
  return `${userName} - ${positionName}`
}

function fmtMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function fmtMinutes(value?: number) {
  const minutes = value || 0
  const hours = Math.floor(minutes / 60)
  const left = minutes % 60
  return `${hours}小时${left}分钟`
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
          v-model="userId"
          filterable
          clearable
          :loading="catalogLoading"
          placeholder="选择志愿者"
          style="width: 260px"
        >
          <el-option
            v-for="item in volunteers"
            :key="item.id"
            :label="`${volunteerLabel(item.id)}（志愿者 ${item.id}）`"
            :value="item.id"
          />
        </el-select>
        <el-button :icon="Search" @click="loadBills">查询账单</el-button>
        <el-button :icon="Refresh" @click="loadBills">刷新</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Plus" @click="openGenerate">生成结算单</el-button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-meta">
        <span>共 {{ bills.length }} 张结算单</span>
        <small>默认按签到与签退记录自动统计工时</small>
      </div>
      <el-table v-loading="loading" :data="bills" row-key="id" highlight-current-row @current-change="showDetail">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column prop="billNo" label="账单号" min-width="190" />
        <el-table-column label="活动" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ activityLabel(row.activityId) }}</strong>
              <span>活动 {{ row.activityId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="志愿者" min-width="170">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ volunteerLabel(row.userId) }}</strong>
              <span>志愿者 {{ row.userId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工时" width="130">
          <template #default="{ row }">{{ fmtMinutes(row.totalWorkMinutes) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">￥{{ fmtMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="billStatus" label="状态" width="110" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Search" @click.stop="showDetail(row)">明细</el-button>
            <el-button link type="success" :icon="Check" @click.stop="confirmBill(row)">确认</el-button>
            <el-button link type="warning" :icon="Money" @click.stop="openPay(row)">付款</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-panel detail-panel">
      <div class="table-meta">
        <span>{{ selectedBill?.billNo || '结算明细' }}</span>
        <small>
          基础 ￥{{ fmtMoney(selectedBill?.baseAmount) }} · 工时 ￥{{ fmtMoney(selectedBill?.hourAmount) }} · 奖励
          ￥{{ fmtMoney(selectedBill?.rewardAmount) }} · 扣款 ￥{{ fmtMoney(selectedBill?.deductAmount) }}
        </small>
      </div>
      <el-table :data="details" row-key="id">
        <el-table-column label="排班对象" min-width="240">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ assignmentLabel(row.assignmentId, row.positionId) }}</strong>
              <span>排班 {{ row.assignmentId }} / 岗位 {{ row.positionId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工时" width="130">
          <template #default="{ row }">{{ fmtMinutes(row.workMinutes) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">￥{{ fmtMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>
    </div>

    <div class="table-panel detail-panel">
      <div class="table-meta">
        <span>付款记录</span>
        <small>共 {{ payments.length }} 条</small>
      </div>
      <el-table :data="payments" row-key="id">
        <el-table-column prop="payNo" label="支付单号" min-width="180" />
        <el-table-column prop="payChannel" label="渠道" width="110" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">￥{{ fmtMoney(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payStatus" label="状态" width="110" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmt(row.payTime) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="generateDialogVisible" title="生成结算单" width="560px">
      <el-form ref="generateFormRef" :model="generateForm" label-width="100px">
        <el-form-item label="活动" prop="activityId" :rules="[{ required: true, message: '请选择活动' }]">
          <el-select
            v-model="generateForm.activityId"
            filterable
            :loading="catalogLoading"
            placeholder="选择活动"
            @change="loadActivityContext"
          >
            <el-option
              v-for="item in activities"
              :key="item.id"
              :label="`${item.name}（活动 ${item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="志愿者" prop="userId" :rules="[{ required: true, message: '请选择志愿者' }]">
          <el-select v-model="generateForm.userId" filterable :loading="catalogLoading" placeholder="选择志愿者">
            <el-option
              v-for="item in volunteers"
              :key="item.id"
              :label="`${volunteerLabel(item.id)}（志愿者 ${item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="基础金额"><el-input-number v-model="generateForm.baseAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="小时单价"><el-input-number v-model="generateForm.hourlyRate" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="奖励金额"><el-input-number v-model="generateForm.rewardAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="扣款金额"><el-input-number v-model="generateForm.deductAmount" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateBill">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDialogVisible" title="登记付款" width="480px">
      <el-form ref="payFormRef" :model="payForm" label-width="88px">
        <el-form-item label="支付渠道" prop="payChannel" :rules="[{ required: true, message: '请选择支付渠道' }]">
          <el-select v-model="payForm.payChannel">
            <el-option label="银行转账" value="BANK" />
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="现金" value="CASH" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付金额" prop="payAmount" :rules="[{ required: true, message: '请输入支付金额' }]">
          <el-input-number v-model="payForm.payAmount" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="payBill">登记</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.detail-panel {
  margin-top: 18px;
}
</style>
