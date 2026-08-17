<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { activityApi, messageApi, volunteerApi } from '@/api/modules'
import type { ActivityRecord, ActivitySignupRecord, MessageNoticeRecord, PageResult, VolunteerRecord } from '@/types/api'

const loading = ref(false)
const receiverId = ref<number>()
const noticeTypeFilter = ref('')
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const notices = ref<MessageNoticeRecord[]>([])
const catalogLoading = ref(false)
const activities = ref<ActivityRecord[]>([])
const volunteers = ref<VolunteerRecord[]>([])
const activitySignups = ref<ActivitySignupRecord[]>([])
const form = reactive({
  targetMode: 'USER',
  activityId: undefined as number | undefined,
  receiverId: undefined as number | undefined,
  noticeType: 'SYSTEM',
  title: '',
  content: '',
  sendChannel: 'IN_APP',
  receiverOpenid: '',
  receiverMobile: '',
})
const volunteerNameMap = computed(() => new Map(volunteers.value.map((item) => [
  item.id,
  item.realName || item.nickname || item.username || `志愿者 ${item.id}`,
])))
const filteredNotices = computed(() => {
  if (!noticeTypeFilter.value) {
    return notices.value
  }
  return notices.value.filter((item) => item.noticeType === noticeTypeFilter.value)
})
const activityReceiverIds = computed(() => Array.from(new Set(activitySignups.value
  .filter((item) => item.signupStatus === 'APPROVED' || item.signupStatus === 'PASS' || item.signupStatus === 'APPROVE')
  .map((item) => item.userId))))
const targetCount = computed(() => {
  if (form.targetMode === 'ALL') {
    return volunteers.value.length
  }
  if (form.targetMode === 'ACTIVITY') {
    return activityReceiverIds.value.length
  }
  return form.receiverId ? 1 : 0
})

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

async function loadNotices() {
  if (!receiverId.value) {
    ElMessage.warning('请选择接收人')
    return
  }
  loading.value = true
  try {
    notices.value = await messageApi.listByReceiver(receiverId.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    targetMode: 'USER',
    activityId: undefined,
    receiverId: receiverId.value,
    noticeType: 'SYSTEM',
    title: '',
    content: '',
    sendChannel: 'IN_APP',
    receiverOpenid: '',
    receiverMobile: '',
  })
  dialogVisible.value = true
}

async function sendNotice() {
  await formRef.value?.validate()
  const receiverIds = resolveReceiverIds()
  if (!receiverIds.length) {
    ElMessage.warning('没有可发送的接收人')
    return
  }
  await Promise.all(receiverIds.map((id) => messageApi.createNotice({
    receiverId: id,
    noticeType: form.noticeType,
    title: form.title,
    content: form.content,
    sendChannel: form.sendChannel,
    receiverOpenid: form.targetMode === 'USER' ? form.receiverOpenid || undefined : undefined,
    receiverMobile: form.targetMode === 'USER' ? form.receiverMobile || undefined : undefined,
  })))
  receiverId.value = receiverIds[0]
  dialogVisible.value = false
  ElMessage.success(`通知已发送给 ${receiverIds.length} 人`)
  await loadNotices()
}

function resolveReceiverIds() {
  if (form.targetMode === 'ALL') {
    return volunteers.value.map((item) => item.id)
  }
  if (form.targetMode === 'ACTIVITY') {
    return activityReceiverIds.value
  }
  return form.receiverId ? [form.receiverId] : []
}

async function loadActivitySignups(activityId?: number) {
  activitySignups.value = []
  if (!activityId) {
    return
  }
  activitySignups.value = await activityApi.signups({ activityId }).catch(() => [])
}

async function markRead(row: MessageNoticeRecord) {
  await messageApi.markRead(row.id)
  ElMessage.success('已标记为已读')
  await loadNotices()
}

function fmt(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function volunteerLabel(userId?: number) {
  if (!userId) {
    return '未指定接收人'
  }
  return volunteerNameMap.value.get(userId) || `志愿者 ${userId}`
}

function noticeTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    SYSTEM: '系统通知',
    SIGNUP: '报名通知',
    SCHEDULE: '排班通知',
    DISPATCH: '调度通知',
    SETTLEMENT: '结算通知',
  }
  return type ? labels[type] || type : '-'
}

function channelLabel(channel?: string) {
  const labels: Record<string, string> = {
    IN_APP: '站内通知',
    WECHAT: '微信',
    SMS: '短信',
  }
  return channel ? labels[channel] || channel : '-'
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="filter-row">
        <el-select
          v-model="receiverId"
          filterable
          clearable
          :loading="catalogLoading"
          placeholder="选择接收人"
          style="width: 260px"
        >
          <el-option
            v-for="item in volunteers"
            :key="item.id"
            :label="`${volunteerLabel(item.id)}（志愿者 ${item.id}）`"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="noticeTypeFilter" clearable placeholder="通知类型" style="width: 160px">
          <el-option label="系统通知" value="SYSTEM" />
          <el-option label="报名通知" value="SIGNUP" />
          <el-option label="排班通知" value="SCHEDULE" />
          <el-option label="调度通知" value="DISPATCH" />
          <el-option label="结算通知" value="SETTLEMENT" />
        </el-select>
        <el-button :icon="Search" @click="loadNotices">查询通知</el-button>
        <el-button :icon="Refresh" @click="loadNotices">刷新</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Plus" @click="openCreate">发送通知</el-button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-meta">
        <span>共 {{ filteredNotices.length }} 条通知</span>
        <small>查询按接收人筛选；发送可按全部志愿者、活动报名人员或指定志愿者</small>
      </div>
      <el-table v-loading="loading" :data="filteredNotices" row-key="id">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column label="接收人" min-width="170">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ volunteerLabel(row.receiverId) }}</strong>
              <span>志愿者 {{ row.receiverId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="125">
          <template #default="{ row }">{{ noticeTypeLabel(row.noticeType) }}</template>
        </el-table-column>
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="渠道" width="110">
          <template #default="{ row }">{{ channelLabel(row.sendChannel) }}</template>
        </el-table-column>
        <el-table-column prop="sendStatus" label="发送状态" width="110" />
        <el-table-column label="已读" width="95">
          <template #default="{ row }">{{ row.isRead ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="发送时间" width="160">
          <template #default="{ row }">{{ fmt(row.sendTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="115" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :icon="Check" :disabled="row.isRead" @click="markRead(row)">已读</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="发送通知" width="620px">
      <el-form ref="formRef" :model="form" label-width="92px">
        <el-form-item label="接收对象">
          <el-radio-group v-model="form.targetMode">
            <el-radio-button label="USER">指定志愿者</el-radio-button>
            <el-radio-button label="ACTIVITY">活动报名人员</el-radio-button>
            <el-radio-button label="ALL">全部志愿者</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="form.targetMode === 'ACTIVITY'"
          label="活动"
          prop="activityId"
          :rules="[{ required: true, message: '请选择活动' }]"
        >
          <el-select
            v-model="form.activityId"
            filterable
            :loading="catalogLoading"
            placeholder="选择活动"
            @change="loadActivitySignups"
          >
            <el-option
              v-for="item in activities"
              :key="item.id"
              :label="`${item.name}（活动 ${item.id}）`"
              :value="item.id"
            />
          </el-select>
          <small class="form-hint">将发送给已通过报名的 {{ activityReceiverIds.length }} 名志愿者</small>
        </el-form-item>
        <el-form-item
          v-if="form.targetMode === 'USER'"
          label="接收人"
          prop="receiverId"
          :rules="[{ required: true, message: '请选择接收人' }]"
        >
          <el-select v-model="form.receiverId" filterable :loading="catalogLoading" placeholder="选择志愿者">
            <el-option
              v-for="item in volunteers"
              :key="item.id"
              :label="`${volunteerLabel(item.id)}（志愿者 ${item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-alert v-if="form.targetMode !== 'USER'" :title="`预计发送 ${targetCount} 人`" type="info" :closable="false" show-icon />
        <el-form-item label="通知类型">
          <el-select v-model="form.noticeType">
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="报名通知" value="SIGNUP" />
            <el-option label="排班通知" value="SCHEDULE" />
            <el-option label="调度通知" value="DISPATCH" />
            <el-option label="结算通知" value="SETTLEMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送渠道">
          <el-select v-model="form.sendChannel">
            <el-option label="站内通知" value="IN_APP" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="短信" value="SMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title" :rules="[{ required: true, message: '请输入标题' }]">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="内容" prop="content" :rules="[{ required: true, message: '请输入内容' }]">
          <el-input v-model="form.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item v-if="form.sendChannel === 'WECHAT'" label="OpenID">
          <el-input v-model="form.receiverOpenid" />
        </el-form-item>
        <el-form-item v-if="form.sendChannel === 'SMS'" label="手机号">
          <el-input v-model="form.receiverMobile" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendNotice">发送</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.form-hint {
  display: block;
  margin-top: 6px;
  color: #7b8b86;
}
</style>
