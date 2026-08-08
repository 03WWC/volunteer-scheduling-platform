<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Check, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { messageApi } from '@/api/modules'
import type { MessageNoticeRecord } from '@/types/api'

const loading = ref(false)
const receiverId = ref<number>()
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const notices = ref<MessageNoticeRecord[]>([])
const form = reactive({
  receiverId: undefined as number | undefined,
  noticeType: 'SYSTEM',
  title: '',
  content: '',
  sendChannel: 'IN_APP',
  receiverOpenid: '',
  receiverMobile: '',
})

async function loadNotices() {
  if (!receiverId.value) {
    ElMessage.warning('请输入接收人编号')
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
  await messageApi.createNotice({
    receiverId: form.receiverId,
    noticeType: form.noticeType,
    title: form.title,
    content: form.content,
    sendChannel: form.sendChannel,
    receiverOpenid: form.receiverOpenid || undefined,
    receiverMobile: form.receiverMobile || undefined,
  })
  receiverId.value = form.receiverId
  dialogVisible.value = false
  ElMessage.success('通知已发送')
  await loadNotices()
}

async function markRead(row: MessageNoticeRecord) {
  await messageApi.markRead(row.id)
  ElMessage.success('已标记为已读')
  await loadNotices()
}

function fmt(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="filter-row">
        <el-input-number v-model="receiverId" :min="1" :controls="false" placeholder="接收人编号" />
        <el-button :icon="Search" @click="loadNotices">查询通知</el-button>
        <el-button :icon="Refresh" @click="loadNotices">刷新</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Plus" @click="openCreate">发送通知</el-button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-meta">
        <span>共 {{ notices.length }} 条通知</span>
        <small>排班发布、调度和人工通知都会沉淀在这里</small>
      </div>
      <el-table v-loading="loading" :data="notices" row-key="id">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column prop="receiverId" label="接收人" width="110" />
        <el-table-column prop="noticeType" label="类型" width="125" />
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sendChannel" label="渠道" width="110" />
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
        <el-form-item label="接收人" prop="receiverId" :rules="[{ required: true, message: '请输入接收人编号' }]">
          <el-input-number v-model="form.receiverId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="通知类型">
          <el-select v-model="form.noticeType">
            <el-option label="系统通知" value="SYSTEM" />
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
