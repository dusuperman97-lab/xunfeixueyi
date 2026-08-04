import http from '@/utils/http.js'

/**
 * 分页查询智能体角色
 */
export function pageAgentRole(query = {}, pageNum = 1, pageSize = 10) {
    return http.get('/agent-role/page', { params: { ...query, pageNum, pageSize } })
}

/**
 * 获取智能体角色列表（仅启用的）
 */
export function listAgentRole() {
    return http.get('/agent-role/list')
}

/**
 * 获取所有智能体角色
 */
export function listAllAgentRole() {
    return http.get('/agent-role/listAll')
}

/**
 * 根据ID获取智能体角色
 */
export function getAgentRoleById(id) {
    return http.get(`/agent-role/selectById/${id}`)
}

/**
 * 新增智能体角色
 */
export function addAgentRole(data) {
    return http.post('/agent-role/add', data)
}

/**
 * 更新智能体角色
 */
export function updateAgentRole(data) {
    return http.put('/agent-role/update', data)
}

/**
 * 批量删除智能体角色
 */
export function delBatchAgentRole(ids) {
    return http.delete('/agent-role/delBatch', { data: ids })
}

/**
 * 获取工作流结构（阶段+智能体分组）
 */
export function getWorkflowStructure() {
    return http.get('/agent-role/workflow-structure')
}

/**
 * 更新智能体的阶段归属
 */
export function updateAgentStage(agentId, stageId, stageName) {
    return http.put('/agent-role/update-stage', { agentId, stageId, stageName })
}
