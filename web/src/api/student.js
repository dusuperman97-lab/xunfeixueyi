import http from '@/utils/http.js'

/**
 * 分页查询学生
 */
export function pageStudent(query = {}, pageNum = 1, pageSize = 9) {
    return http.get('/student/page', { params: { ...query, pageNum, pageSize } })
}

/**
 * 获取学生列表
 */
export function listStudent() {
    return http.get('/student/list')
}

/**
 * 根据ID获取学生
 */
export function getStudentById(id) {
    return http.get(`/student/${id}`)
}

/**
 * 新增学生
 */
export function addStudent(data) {
    return http.post('/student/add', data)
}

/**
 * 更新学生
 */
export function updateStudent(data) {
    return http.put('/student/update', data)
}

/**
 * 批量删除学生（接口为 POST）
 */
export function deleteStudent(ids) {
    return http.post('/student/delete', ids)
}
