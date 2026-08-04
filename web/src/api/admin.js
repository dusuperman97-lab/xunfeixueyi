import http from '@/utils/http.js'

/**
 * 获取控制台统计数据
 */
export function getDashboardStats() {
    return http.get('/admin/dashboard/stats')
}

/**
 * 获取仪表盘图表数据（用户趋势等）
 */
export function getDashboardCharts() {
    return http.get('/admin/dashboard/charts')
}
