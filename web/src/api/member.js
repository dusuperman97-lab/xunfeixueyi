import http from '@/utils/http.js'

// 查询当前用户积分余额
export function getBalance() {
    return http.get('/member-points/balance')
}

// 查询指定用户积分余额（管理员）
export function getBalanceByUserId(userId) {
    return http.get(`/member-points/balance/${userId}`)
}

// 创建支付宝支付订单
export function createAlipayPayment(points, totalAmount, description = '积分充值') {
    return http.post('/alipay/create-payment', { points, totalAmount, description })
}

// 查询支付订单状态
export function queryPaymentOrder(outTradeNo) {
    return http.get('/alipay/query-order', { params: { outTradeNo } })
}

// 管理员调整用户积分
export function adminAdjust(userId, points, description = '管理员调整') {
    return http.post('/member-points/admin-adjust', { userId, points, description })
}

// 分页查询当前用户积分流水
export function getTransactions(query = {}, pageNum = 1, pageSize = 10) {
    return http.get('/member-points/transactions', {
        params: {
            ...query,
            pageNum,
            pageSize
        }
    })
}

// 管理员分页查询指定用户积分流水
export function getTransactionsByUserId(userId, query = {}, pageNum = 1, pageSize = 10) {
    return http.get(`/member-points/transactions/${userId}`, {
        params: {
            ...query,
            pageNum,
            pageSize
        }
    })
}

// 管理端：分页查询所有用户积分（含用户名）
export function getAdminMemberPointsList(username = '', pageNum = 1, pageSize = 10) {
    return http.get('/member-points/admin/list', {
        params: { username, pageNum, pageSize }
    })
}

// 管理端：分页查询所有积分流水（不限用户）
export function getAdminAllTransactions(username = '', transactionType = '', bizType = '', pageNum = 1, pageSize = 10) {
    return http.get('/member-points/admin/transactions', {
        params: { username, transactionType, bizType, pageNum, pageSize }
    })
}
