package com.project.platform.service;

import com.project.platform.entity.MemberPoints;
import com.project.platform.entity.PointsTransaction;
import com.project.platform.vo.MemberPointsVO;
import com.project.platform.vo.PageVO;

/**
 * 会员积分服务
 */
public interface MemberPointsService {

    /**
     * 获取用户积分余额
     */
    MemberPoints getByUserId(Integer userId);

    /**
     * 确保用户积分记录存在（不存在则初始化）
     */
    MemberPoints ensurePointsRecord(Integer userId);

    /**
     * 扣减积分（内部校验余额）
     *
     * @param points  扣减积分数量
     * @param bizType 业务类型
     * @throws com.project.platform.exception.CustomException 余额不足时抛出
     */
    void deductPoints(Integer points, String bizType);

    /**
     * 扣减积分（指定用户）
     *
     * @param userId  用户ID
     * @param points  扣减积分数量
     * @param bizType 业务类型
     */
    void deductPoints(Integer userId, Integer points, String bizType);

    /**
     * 充值积分
     *
     * @param userId      用户ID
     * @param points      充值积分数量
     * @param description 充值描述
     */
    void rechargePoints(Integer userId, Integer points, String description);

    /**
     * 退还积分（业务执行失败时调用）
     *
     * @param points  退还积分数量
     * @param bizType 业务类型
     * @param reason  退还原因
     */
    void refundPoints(Integer points, String bizType, String reason);

    /**
     * 退还积分（指定用户）
     *
     * @param userId  用户ID
     * @param points  退还积分数量
     * @param bizType 业务类型
     * @param reason  退还原因
     */
    void refundPoints(Integer userId, Integer points, String bizType, String reason);

    /**
     * 管理员调整积分（可增可减）
     *
     * @param userId      用户ID
     * @param points      调整积分数量（正数为增加，负数为扣减）
     * @param description 调整说明
     */
    void adminAdjustPoints(Integer userId, Integer points, String description);

    /**
     * 分页查询积分流水
     */
    PageVO<PointsTransaction> queryTransactions(Integer userId, String transactionType,
                                                 String bizType, Integer pageNum, Integer pageSize);

    /**
     * 管理端：分页查询所有用户积分（含用户名）
     */
    PageVO<MemberPointsVO> queryAllMemberPoints(String username, Integer pageNum, Integer pageSize);

    /**
     * 管理端：分页查询所有积分流水（不限用户）
     */
    PageVO<PointsTransaction> queryAllTransactions(String username, String transactionType,
                                                    String bizType, Integer pageNum, Integer pageSize);
}
