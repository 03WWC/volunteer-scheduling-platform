<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { activityApi } from '@/api/modules'
import type { ActivityRecord, PageResult } from '@/types/api'

const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const activities = ref<ActivityRecord[]>([])
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  activityType: 'VOLUNTEER',
  location: '',
  ownerName: '',
  contactPhone: '',
  startTime: '',
  endTime: '',
})

const dialogTitle = computed(() => form.id ? '编辑活动' : '新建活动')
const visibleActivities = computed(() => activities.value.filter((item) => {
  const text = `${item.id} ${item.name} ${item.location || item.address || ''} ${item.ownerName || ''}`
  return !keyword.value || text.includes(keyword.value)
}))

onMounted(loadActivities)

async function loadActivities() {
  loading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 100, keyword: keyword.value || undefined })
    activities.value = Array.isArray(result) ? result : (result as PageResult<ActivityRecord>).records || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: undefined, name: '', activityType: 'VOLUNTEER', location: '', ownerName: '', contactPhone: '', startTime: '', endTime: '' })
  dialogVisible.value = true
}

function openEdit(row: ActivityRecord) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    activityType: row.activityType || 'VOLUNTEER',
    location: row.location || row.address || '',
    ownerName: row.ownerName || '',
    contactPhone: row.contactPhone || '',
    startTime: toDateTime(row.startTime),
    endTime: toDateTime(row.endTime),
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const payload = { ...form, startTime: toApiTime(form.startTime), endTime: toApiTime(form.endTime) }
  if (form.id) {
    await activityApi.update(payload)
    ElMessage.success('活动已更新')
  } else {
    await activityApi.create(payload)
    ElMessage.success('活动已创建')
  }
  dialogVisible.value = false
  await loadActivities()
}

async function remove(row: ActivityRecord) {
  await ElMessageBox.confirm(`确认删除活动「${row.name}」？`, '删除活动', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  await activityApi.remove(row.id)
  ElMessage.success('活动已删除')
  await loadActivities()
}

function toDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function toApiTime(value: string) {
  return value ? value.replace(' ', 'T') : undefined
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="filter-row">
        <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索活动名称、地点、负责人" @change="loadActivities" />
        <el-button :icon="Refresh" @click="loadActivities">刷新</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建活动</el-button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-meta"><span>共 {{ visibleActivities.length }} 条活动记录</span><small>活动用于报名、岗位和排班</small></div>
      <el-table v-loading="loading" :data="visibleActivities" row-key="id">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column label="活动" min-width="240">
          <template #default="{ row }"><div class="primary-cell"><strong>{{ row.name }}</strong><span>{{ row.location || row.address || '地点待定' }}</span></div></template>
        </el-table-column>
        <el-table-column prop="activityType" label="类型" width="120" />
        <el-table-column prop="ownerName" label="负责人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column label="时间" min-width="210">
          <template #default="{ row }">{{ toDateTime(row.startTime) }} - {{ toDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="155" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form ref="formRef" :model="form" label-width="92px">
        <el-form-item label="活动名称" prop="name" :rules="[{ required: true, message: '请输入活动名称' }]"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="活动类型"><el-input v-model="form.activityType" /></el-form-item>
        <el-form-item label="活动地点" prop="location" :rules="[{ required: true, message: '请输入活动地点' }]"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.ownerName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm" format="YYYY-MM-DD HH:mm" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm" format="YYYY-MM-DD HH:mm" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>
