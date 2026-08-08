import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AuthSession, VolunteerProfile } from '@/types/api'

const PROFILE_KEY = 'volunteer-profile'
const TOKEN_KEY = 'volunteer-token'

export const useUserStore = defineStore('user', () => {
  const saved = uni.getStorageSync(PROFILE_KEY)
  const profile = ref<VolunteerProfile | null>(saved || null)
  const isLoggedIn = computed(() => Boolean(profile.value?.id))

  function setSession(value: AuthSession) {
    profile.value = value.user
    uni.setStorageSync(PROFILE_KEY, value.user)
    uni.setStorageSync(TOKEN_KEY, value.token)
  }

  function updateProfile(value: Partial<VolunteerProfile>) {
    if (!profile.value) {
      return
    }
    profile.value = { ...profile.value, ...value }
    uni.setStorageSync(PROFILE_KEY, profile.value)
  }

  function logout() {
    profile.value = null
    uni.removeStorageSync(PROFILE_KEY)
    uni.removeStorageSync(TOKEN_KEY)
  }

  return { profile, isLoggedIn, setSession, updateProfile, logout }
})
