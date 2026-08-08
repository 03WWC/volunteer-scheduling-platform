<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type TreeInstance } from 'element-plus'
import { adminApi } from '@/api/modules'
import type { AdminPermissionTreeRecord, AdminRoleRecord, AdminUserRecord } from '@/types/api'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const permissionTreeRef = ref<TreeInstance>()
const admins = ref<AdminUserRecord[]>([])
const roles = ref<AdminRoleRecord[]>([])
const permissions = ref<AdminPermissionTreeRecord[]>([])
const selectedRoleId = ref<number>()
const form = reactive({
  id: undefined as number | undefined,
  account: '',
  password: '',
  username: '',
  realName: '',
  mobile: '',
  status: 'ENABLED',
  roleIds: [] as number[],
})

const selectedRole = computed(() => roles.value.find((role) => role.id === selectedRoleId.value))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [adminRows, roleRows, permissionRows] = await Promise.all([
      adminApi.listAdmins(),
      adminApi.listRoles(),
      adminApi.permissionTree(),
    ])
    admins.value = adminRows
    roles.value = roleRows
    permissions.value = permissionRows
    selectedRoleId.value = selectedRoleId.value || roleRows[0]?.id
    syncCheckedPermissions()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    account: '',
    password: '',
    username: '',
    realName: '',
    mobile: '',
    status: 'ENABLED',
    roleIds: [],
  })
  dialogVisible.value = true
}

function openEdit(row: AdminUserRecord) {
  Object.assign(form, {
    id: row.id,
    account: row.account,
    password: '',
    username: row.username,
    realName: row.realName || '',
    mobile: row.mobile || '',
    status: row.status,
    roleIds: row.roles.map((role) => role.id),
  })
  dialogVisible.value = true
}

async function submitAdmin() {
  await formRef.value?.validate()
  await adminApi.saveAdmin({
    id: form.id,
    account: form.account,
    password: form.password || undefined,
    username: form.username || form.account,
    realName: form.realName || undefined,
    mobile: form.mobile || undefined,
    status: form.status,
    roleIds: form.roleIds,
  })
  ElMessage.success('管理员已保存')
  dialogVisible.value = false
  await loadAll()
}

function onRoleChange() {
  syncCheckedPermissions()
}

function syncCheckedPermissions() {
  requestAnimationFrame(() => {
    permissionTreeRef.value?.setCheckedKeys(selectedRole.value?.permissionIds || [])
  })
}

async function saveRolePermissions() {
  if (!selectedRoleId.value) {
    return
  }
  const checkedKeys = permissionTreeRef.value?.getCheckedKeys(false) || []
  const halfCheckedKeys = permissionTreeRef.value?.getHalfCheckedKeys() || []
  await adminApi.updateRolePermissions({
    roleId: selectedRoleId.value,
    permissionIds: [...checkedKeys, ...halfCheckedKeys].map(Number),
  })
  ElMessage.success('角色权限已保存')
  await loadAll()
}

function roleNames(row: AdminUserRecord) {
  return row.roles.map((role) => role.roleName).join('、') || '未分配'
}
</script>

<template>
  <section>
    <div class="toolbar">
      <div class="filter-row">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
      <div class="command-row">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增管理员</el-button>
      </div>
    </div>

    <div class="system-grid">
      <div class="table-panel">
        <div class="table-meta">
          <span>管理员账号</span>
          <small>发布活动、审核、排班、签到等后台人员在这里分配角色</small>
        </div>
        <el-table v-loading="loading" :data="admins" row-key="id">
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column label="账号" min-width="180">
            <template #default="{ row }">
              <div class="primary-cell">
                <strong>{{ row.realName || row.username || row.account }}</strong>
                <span>{{ row.account }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="mobile" label="手机号" min-width="130" />
          <el-table-column label="角色" min-width="220">
            <template #default="{ row }">{{ roleNames(row) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-panel">
        <div class="table-meta">
          <span>角色权限树</span>
          <small>切换角色后勾选可访问模块</small>
        </div>
        <div class="permission-panel">
          <el-select v-model="selectedRoleId" placeholder="选择角色" @change="onRoleChange">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
          <el-tree
            ref="permissionTreeRef"
            :data="permissions"
            node-key="id"
            show-checkbox
            default-expand-all
            :props="{ label: 'permissionName', children: 'children' }"
          />
          <el-button type="primary" @click="saveRolePermissions">保存角色权限</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="620px">
      <el-form ref="formRef" :model="form" label-width="92px">
        <el-form-item label="账号" prop="account" :rules="[{ required: true, message: '请输入账号' }]">
          <el-input v-model="form.account" :disabled="Boolean(form.id)" />
        </el-form-item>
        <el-form-item label="密码" prop="password" :rules="form.id ? [] : [{ required: true, message: '请输入密码' }]">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '不填则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ENABLED">启用</el-radio-button>
            <el-radio-button label="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdmin">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.system-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, .8fr);
  gap: 14px;
}

.permission-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.permission-panel .el-select {
  width: 100%;
}

@media (max-width: 1100px) {
  .system-grid {
    grid-template-columns: 1fr;
  }
}
</style>
