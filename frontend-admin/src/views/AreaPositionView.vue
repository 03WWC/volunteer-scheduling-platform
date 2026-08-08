<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, areaApi, positionApi } from '@/api/modules'
import { POSITION_SKILL_OPTIONS, normalizeSkillCode, skillNameOf } from '@/constants/skills'
import type { ActivityRecord, AreaRecord, PageResult, PositionRecord } from '@/types/api'

const route = useRoute()
const activeTab = ref(String(route.meta.tab || 'area'))
const activityId = ref<number>()
const keyword = ref('')
const loading = ref(false)
const activityLoading = ref(false)
const dialogVisible = ref(false)
const activities = ref<ActivityRecord[]>([])
const areas = ref<AreaRecord[]>([])
const positions = ref<PositionRecord[]>([])
const form = reactive<Record<string, any>>({})
const isArea = computed(() => activeTab.value === 'area')
const rows = computed(() => isArea.value ? areas.value : positions.value)
const dialogTitle = computed(() => `${form.id ? '编辑' : '新建'}${isArea.value ? '服务区域' : '岗位'}`)
const visibleRows = computed(() => rows.value.filter((row: any) => {
  if (!keyword.value) {
    return true
  }
  const skillText = isArea.value ? '' : `${row.skillRequirement || ''} ${skillNameOf(row.skillRequirement)}`
  return `${row.id} ${row.name} ${row.ownerName || ''} ${skillText}`.includes(keyword.value)
}))
const areaNameMap = computed(() => new Map(areas.value.map((area) => [area.id, area.name])))

watch(() => route.meta.tab, (tab) => { activeTab.value = String(tab || 'area') })
onMounted(loadActivities)

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await activityApi.page({ pageNo: 1, pageSize: 100 })
    activities.value = Array.isArray(result) ? result : (result as PageResult<ActivityRecord>).records || []
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
    ElMessage.info('请先选择活动')
    return
  }
  loading.value = true
  try {
    areas.value = await areaApi.list(activityId.value)
    positions.value = await positionApi.list(activityId.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!activityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, isArea.value
    ? { activityId: activityId.value, name: '', gps: '', ownerName: '' }
    : { activityId: activityId.value, areaId: undefined, name: '', needCount: 1, skillRequirement: 'NONE', salary: 0, startTime: '', endTime: '' })
  dialogVisible.value = true
}

function openEdit(row: AreaRecord | PositionRecord) {
  Object.keys(form).forEach((key) => delete form[key])
  const position = row as PositionRecord
  Object.assign(form, row, {
    startTime: toDateTime(position.startTime),
    endTime: toDateTime(position.endTime),
    ...(isArea.value ? {} : { skillRequirement: normalizeSkillCode(position.skillRequirement) || 'NONE' }),
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.activityId) {
    ElMessage.warning('请先选择活动')
    return
  }
  const payload = {
    ...form,
    startTime: toApiTime(form.startTime),
    endTime: toApiTime(form.endTime),
    ...(!isArea.value ? { skillRequirement: normalizeSkillCode(form.skillRequirement) || 'NONE' } : {}),
  }
  if (isArea.value) {
    form.id ? await areaApi.update(payload) : await areaApi.create(payload)
  } else {
    form.id ? await positionApi.update(payload) : await positionApi.create(payload)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  await loadData()
}

function areaLabel(areaId?: number) {
  return areaId ? areaNameMap.value.get(areaId) || `区域 ${areaId}` : '-'
}

async function remove(row: AreaRecord | PositionRecord) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  isArea.value ? await areaApi.remove(row.id) : await positionApi.remove(row.id)
  ElMessage.success('已删除')
  await loadData()
}

function toDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function toApiTime(value?: string) {
  return value ? value.replace(' ', 'T') : undefined
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
          style="width: 260px"
          @change="loadData"
        >
          <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
        </el-select>
        <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索名称" />
        <el-button :icon="Refresh" @click="loadData">查询/刷新</el-button>
      </div>
      <div class="command-row"><el-button type="primary" :icon="Plus" @click="openCreate">新建{{ isArea ? '区域' : '岗位' }}</el-button></div>
    </div>

    <el-tabs v-model="activeTab" class="tabs-panel">
      <el-tab-pane label="服务区域" name="area" />
      <el-tab-pane label="岗位管理" name="position" />
    </el-tabs>
    <div class="table-panel">
      <div class="table-meta"><span>共 {{ visibleRows.length }} 条{{ isArea ? '区域' : '岗位' }}记录</span><small>按活动编号维护</small></div>
      <el-table v-loading="loading" :data="visibleRows" row-key="id">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column v-if="isArea" prop="gps" label="GPS" min-width="180" />
        <el-table-column v-if="isArea" prop="ownerName" label="负责人" width="130" />
        <el-table-column v-if="!isArea" label="服务区域" min-width="150">
          <template #default="{ row }">{{ areaLabel(row.areaId) }}</template>
        </el-table-column>
        <el-table-column v-if="!isArea" prop="needCount" label="人数" width="90" />
        <el-table-column v-if="!isArea" label="技能要求" min-width="160">
          <template #default="{ row }"><el-tag effect="light">{{ skillNameOf(row.skillRequirement) }}</el-tag></template>
        </el-table-column>
        <el-table-column v-if="!isArea" prop="salary" label="补贴" width="100" />
        <el-table-column label="操作" width="155" fixed="right">
          <template #default="{ row }"><el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button><el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="所属活动">
          <el-select v-model="form.activityId" filterable style="width: 100%">
            <el-option v-for="item in activities" :key="item.id" :label="`${item.name}（${item.id}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <template v-if="isArea">
          <el-form-item label="GPS"><el-input v-model="form.gps" /></el-form-item>
          <el-form-item label="负责人"><el-input v-model="form.ownerName" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="服务区域">
            <el-select v-model="form.areaId" filterable clearable style="width: 100%">
              <el-option v-for="area in areas" :key="area.id" :label="`${area.name}（${area.id}）`" :value="area.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="所需人数"><el-input-number v-model="form.needCount" :min="1" /></el-form-item>
          <el-form-item label="技能要求">
            <el-select v-model="form.skillRequirement" filterable style="width: 100%">
              <el-option
                v-for="item in POSITION_SKILL_OPTIONS"
                :key="item.code"
                :label="item.name"
                :value="item.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="补贴"><el-input-number v-model="form.salary" :min="0" /></el-form-item>
          <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm" format="YYYY-MM-DD HH:mm" /></el-form-item>
          <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm" format="YYYY-MM-DD HH:mm" /></el-form-item>
        </template>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.tabs-panel { margin-top: 14px; padding: 0 16px; border: 1px solid var(--line); border-radius: 7px; background: white; }
</style>
