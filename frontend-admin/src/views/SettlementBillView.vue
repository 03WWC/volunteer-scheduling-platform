<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Check, Money, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { settlementApi } from '@/api/modules'
import type { SettlementBillRecord } from '@/types/api'

const loading = ref(false)
const userId = ref<number>()
const selectedBill = ref<SettlementBillRecord>()
const bills = ref<SettlementBillRecord[]>([])
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

async function loadBills() {
  if (!userId.value) {
    ElMessage.warning('请输入志愿者编号')
    return
  }
  loading.value = true
  try {
    bills.value = await settlementApi.listByUser(userId.value)
    selectedBill.value = bills.value[0]
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
        <el-input-number v-model="userId" :min="1" :controls="false" placeholder="志愿者编号" />
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
        <el-table-column prop="activityId" label="活动编号" width="110" />
        <el-table-column prop="userId" label="志愿者编号" width="125" />
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
        <el-table-column prop="assignmentId" label="安排编号" width="110" />
        <el-table-column prop="positionId" label="岗位编号" width="110" />
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
        <el-form-item label="活动编号" prop="activityId" :rules="[{ required: true, message: '请输入活动编号' }]">
          <el-input-number v-model="generateForm.activityId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="志愿者编号" prop="userId" :rules="[{ required: true, message: '请输入志愿者编号' }]">
          <el-input-number v-model="generateForm.userId" :min="1" :controls="false" />
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
