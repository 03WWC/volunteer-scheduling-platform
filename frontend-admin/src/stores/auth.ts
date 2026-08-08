import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/modules'

const TOKEN_KEY = 'admin-token'
const NAME_KEY = 'admin-name'
const ROLES_KEY = 'admin-roles'
const PERMISSIONS_KEY = 'admin-permissions'

function readJsonArray(key: string) {
  try {
    const value = localStorage.getItem(key)
    return value ? (JSON.parse(value) as string[]) : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const displayName = ref(localStorage.getItem(NAME_KEY) || 'admin')
  const roles = ref<string[]>(readJsonArray(ROLES_KEY))
  const permissions = ref<string[]>(readJsonArray(PERMISSIONS_KEY))
  const isLoggedIn = computed(() => Boolean(token.value))
  const isSuperAdmin = computed(() => roles.value.includes('SUPER_ADMIN'))

  async function login(account: string, password: string) {
    const session = await authApi.adminLogin({ account, password })
    token.value = session.token
    displayName.value = session.user.realName || session.user.username || account
    roles.value = session.roles || []
    permissions.value = session.permissions || []
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(NAME_KEY, displayName.value)
    localStorage.setItem(ROLES_KEY, JSON.stringify(roles.value))
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(permissions.value))
  }

  function logout() {
    token.value = ''
    displayName.value = 'admin'
    roles.value = []
    permissions.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NAME_KEY)
    localStorage.removeItem(ROLES_KEY)
    localStorage.removeItem(PERMISSIONS_KEY)
  }

  function hasPermission(permission?: string) {
    return !permission || isSuperAdmin.value || permissions.value.includes(permission)
  }

  return { token, displayName, roles, permissions, isLoggedIn, isSuperAdmin, hasPermission, login, logout }
})
