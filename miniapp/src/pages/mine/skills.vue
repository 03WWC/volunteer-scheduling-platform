<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { userApi } from '@/api/modules'
import { SKILL_LEVEL_OPTIONS, VOLUNTEER_SKILL_OPTIONS, skillDisplayName, skillLevelNameOf } from '@/constants/skills'
import { useUserStore } from '@/stores/user'
import type { UserSkillItem } from '@/types/api'

const userStore = useUserStore()
const loading = ref(false)
const skills = ref<UserSkillItem[]>([])
const skillIndex = ref(0)
const levelIndex = ref(0)
const form = reactive({
  skillCode: VOLUNTEER_SKILL_OPTIONS[0]?.code || '',
  skillName: VOLUNTEER_SKILL_OPTIONS[0]?.name || '',
  skillLevel: 'BEGINNER',
})
const skillNames = VOLUNTEER_SKILL_OPTIONS.map((item) => item.name)
const levelNames = SKILL_LEVEL_OPTIONS.map((item) => item.name)
const selectedSkill = computed(() => VOLUNTEER_SKILL_OPTIONS[skillIndex.value] || VOLUNTEER_SKILL_OPTIONS[0])
const selectedLevel = computed(() => SKILL_LEVEL_OPTIONS[levelIndex.value] || SKILL_LEVEL_OPTIONS[0])

onMounted(loadSkills)

async function loadSkills() {
  if (!userStore.profile?.id) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    skills.value = await userApi.listSkills(userStore.profile.id)
  } finally {
    loading.value = false
  }
}

async function saveSkill() {
  if (!userStore.profile?.id) {
    return
  }
  if (!selectedSkill.value) {
    uni.showToast({ title: '请选择技能标签', icon: 'none' })
    return
  }
  await userApi.saveSkill({
    userId: userStore.profile.id,
    skillCode: selectedSkill.value.code,
    skillName: selectedSkill.value.name,
    skillLevel: selectedLevel.value.code,
  })
  resetForm()
  uni.showToast({ title: '技能已保存', icon: 'success' })
  await loadSkills()
}

function onSkillChange(event: { detail: { value: number | string } }) {
  const index = Number(event.detail.value)
  if (Number.isInteger(index) && index >= 0 && index < VOLUNTEER_SKILL_OPTIONS.length) {
    skillIndex.value = index
    form.skillCode = VOLUNTEER_SKILL_OPTIONS[index].code
    form.skillName = VOLUNTEER_SKILL_OPTIONS[index].name
  }
}

function onLevelChange(event: { detail: { value: number | string } }) {
  const index = Number(event.detail.value)
  if (Number.isInteger(index) && index >= 0 && index < SKILL_LEVEL_OPTIONS.length) {
    levelIndex.value = index
    form.skillLevel = SKILL_LEVEL_OPTIONS[index].code
  }
}

function resetForm() {
  skillIndex.value = 0
  levelIndex.value = 0
  Object.assign(form, {
    skillCode: VOLUNTEER_SKILL_OPTIONS[0]?.code || '',
    skillName: VOLUNTEER_SKILL_OPTIONS[0]?.name || '',
    skillLevel: SKILL_LEVEL_OPTIONS[0]?.code || 'BEGINNER',
  })
}
</script>

<template>
  <view class="page">
    <view class="form-card surface">
      <view class="field">
        <text>技能标签</text>
        <picker mode="selector" :range="skillNames" :value="skillIndex" @change="onSkillChange">
          <view class="picker-value">{{ selectedSkill?.name || '请选择技能' }}</view>
        </picker>
      </view>
      <view class="field">
        <text>技能等级</text>
        <picker mode="selector" :range="levelNames" :value="levelIndex" @change="onLevelChange">
          <view class="picker-value">{{ selectedLevel?.name || '请选择等级' }}</view>
        </picker>
      </view>
      <button class="primary-button" @click="saveSkill">保存技能</button>
    </view>

    <view class="skill-list surface">
      <view v-for="item in skills" :key="item.id" class="skill-item">
        <view><text>{{ skillDisplayName(item.skillCode, item.skillName) }}</text></view>
        <text class="status">{{ skillLevelNameOf(item.skillLevel) }}</text>
      </view>
      <view v-if="loading" class="empty-state">加载中...</view>
      <view v-else-if="!skills.length" class="empty-state">暂无技能标签</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.form-card, .skill-list { padding: 28rpx; }
.skill-list { margin-top: 22rpx; }
.field { margin-bottom: 22rpx; }
.field text { display: block; margin-bottom: 12rpx; color: #52615d; font-size: 24rpx; }
.picker-value { height: 78rpx; padding: 0 20rpx; display: flex; align-items: center; border: 1rpx solid #dfe8e5; border-radius: 10rpx; background: #f8fbfa; font-size: 25rpx; color: #1d332e; box-sizing: border-box; }
.primary-button { width: 100%; height: 82rpx; margin-top: 18rpx; border-radius: 10rpx; background: #286f60; color: white; font-size: 27rpx; line-height: 82rpx; }
.skill-item { min-height: 86rpx; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; border-bottom: 1rpx solid #e6ecea; }
.skill-item:last-child { border-bottom: 0; }
.skill-item view { min-width: 0; }
.skill-item view text { display: block; }
.skill-item view text:first-child { font-size: 27rpx; font-weight: 650; }
.skill-item view text:last-child { margin-top: 8rpx; color: #7b8985; font-size: 21rpx; }
</style>
