<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { AlarmClock, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { volunteerApi } from '@/api/modules'
import { SKILL_LEVEL_OPTIONS, VOLUNTEER_SKILL_OPTIONS, skillDisplayName, skillLevelNameOf, skillNameOf } from '@/constants/skills'
import type { UserAvailabilityRecord, UserSkillRecord, VolunteerRecord } from '@/types/api'

const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const volunteers = ref<VolunteerRecord[]>([])
const skills = ref<UserSkillRecord[]>([])
const availability = ref<UserAvailabilityRecord[]>([])
const selectedVolunteer = ref<VolunteerRecord>()
const form = reactive({
  username: '',
  mobile: '',
  realName: '',
  idCardNo: '',
  skillCode: '',
  skillLevel: 'INTERMEDIATE',
  availableDate: '',
  startTime: '',
  endTime: '',
})

const visibleVolunteers = computed(() => volunteers.value.filter((item) => {
  const text = `${item.id} ${item.username || ''} ${item.realName || ''} ${item.mobile || ''} ${item.authStatus || ''}`
  return !keyword.value || text.includes(keyword.value)
}))

onMounted(loadVolunteers)

async function loadVolunteers() {
  loading.value = true
  try {
    volunteers.value = await volunteerApi.list({ mobileKeyword: keyword.value || undefined })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    username: '',
    mobile: '',
    realName: '',
    idCardNo: '',
    skillCode: '',
    skillLevel: 'INTERMEDIATE',
    availableDate: '',
    startTime: '',
    endTime: '',
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const user = await volunteerApi.register({
    username: form.username,
    mobile: form.mobile,
    userType: 'VOLUNTEER',
  })
  if (form.realName) {
    await volunteerApi.authenticate({
      userId: user.id,
      realName: form.realName,
      idCardNo: form.idCardNo || `TEST${user.id}`,
    })
  }
  if (form.skillCode) {
    await volunteerApi.saveSkill({
      userId: user.id,
      skillCode: form.skillCode,
      skillName: skillNameOf(form.skillCode),
      skillLevel: form.skillLevel,
    })
  }
  if (form.availableDate && form.startTime && form.endTime) {
    await volunteerApi.saveAvailability({
      userId: user.id,
      availableDate: form.availableDate,
      startTime: toApiTime(form.startTime),
      endTime: toApiTime(form.endTime),
    })
  }
  ElMessage.success('志愿者已创建，可用于自动排班')
  dialogVisible.value = false
  await loadVolunteers()
}

async function openDetail(row: VolunteerRecord) {
  selectedVolunteer.value = row
  detailVisible.value = true
  skills.value = await volunteerApi.listSkills(row.id)
  availability.value = await volunteerApi.listAvailability(row.id)
}

function authLabel(status?: number | string) {
  if (status === 1 || status === 'AUTHENTICATED') {
    return '已认证'
  }
  return status ? String(status) : '未认证'
}

function toApiTime(value: string) {
  return value ? value.replace(' ', 'T') : undefined
}

function fmt(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="filter-row">
        <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索姓名、手机号、编号" @change="loadVolunteers" />
        <el-button :icon="Refresh" @click="loadVolunteers">刷新</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增志愿者</el-button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-meta">
        <span>共 {{ visibleVolunteers.length }} 名志愿者</span>
        <small>技能和空闲时间会参与自动排班</small>
      </div>
      <el-table v-loading="loading" :data="visibleVolunteers" row-key="id">
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column label="志愿者" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">
              <strong>{{ row.realName || row.username || `志愿者 ${row.id}` }}</strong>
              <span>{{ row.mobile || '未留手机号' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="认证状态" width="120">
          <template #default="{ row }">
            <el-tag :type="authLabel(row.authStatus) === '已认证' ? 'success' : 'warning'" effect="light">{{ authLabel(row.authStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="AlarmClock" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新增志愿者" width="620px">
      <el-form ref="formRef" :model="form" label-width="96px">
        <el-form-item label="用户名" prop="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile" :rules="[{ required: true, message: '请输入手机号' }]">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCardNo" />
        </el-form-item>
        <el-form-item label="技能标签">
          <el-select v-model="form.skillCode" filterable clearable placeholder="不填则只按时间匹配" style="width: 100%">
            <el-option
              v-for="item in VOLUNTEER_SKILL_OPTIONS"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="技能等级">
          <el-select v-model="form.skillLevel" style="width: 100%">
            <el-option v-for="item in SKILL_LEVEL_OPTIONS" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="可服务日期">
          <el-date-picker v-model="form.availableDate" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm" format="YYYY-MM-DD HH:mm" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm" format="YYYY-MM-DD HH:mm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="selectedVolunteer?.realName || selectedVolunteer?.username || '志愿者详情'" width="720px">
      <div class="detail-grid">
        <div>
          <h3>技能</h3>
          <el-table :data="skills" size="small" row-key="id">
            <el-table-column label="技能">
              <template #default="{ row }">{{ skillDisplayName(row.skillCode, row.skillName) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="120">
              <template #default="{ row }">{{ skillLevelNameOf(row.skillLevel) }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div>
          <h3>空闲时间</h3>
          <el-table :data="availability" size="small" row-key="id">
            <el-table-column prop="availableDate" label="日期" width="120" />
            <el-table-column label="时间">
              <template #default="{ row }">{{ fmt(row.startTime) }} - {{ fmt(row.endTime) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.detail-grid {
  display: grid;
  gap: 18px;
}

.detail-grid h3 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 700;
}
</style>
