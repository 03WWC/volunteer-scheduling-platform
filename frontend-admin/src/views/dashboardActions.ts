const metricRoutes: Record<string, string> = {
  活动总数: '/activity/list',
  在岗志愿者: '/location/checkins',
  待处理缺口: '/dispatch/shortage',
  计划服务时长: '/schedule/plans',
}

export function resolveMetricRoute(label: string) {
  return metricRoutes[label] || '/dashboard'
}

export function resolveAlertRoute(title: string) {
  if (title.includes('整体稳定')) {
    return '/schedule/plans'
  }
  if (title.includes('缺口')) {
    return '/dispatch/shortage'
  }
  if (title.includes('签到')) {
    return '/location/checkins'
  }
  return '/schedule/plans'
}
