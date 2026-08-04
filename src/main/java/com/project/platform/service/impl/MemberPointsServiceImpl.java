package com.project.platform.service.impl;

import com.project.platform.entity.MemberPoints;
import com.project.platform.entity.PointsTransaction;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.MemberPointsMapper;
import com.project.platform.mapper.PointsTransactionMapper;
import com.project.platform.service.MemberPointsService;
import com.project.platform.utils.CurrentUserThreadLocal;
import com.project.platform.vo.MemberPointsVO;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员积分服务实现
 */
@Service
@Slf4j
public class MemberPointsServiceImpl implements MemberPointsService {

    @Resource
    private MemberPointsMapper memberPointsMapper;

    @Resource
    private PointsTransactionMapper pointsTransactionMapper;

    @Value("${member-points.initial-points:100}")
    private Integer initialPoints;

    @Override
    public MemberPoints getByUserId(Integer userId) {
        return memberPointsMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public MemberPoints ensurePointsRecord(Integer userId) {
        MemberPoints points = memberPointsMapper.selectByUserId(userId);
        if (points == null) {
            points = new MemberPoints();
            points.setUserId(userId);
            points.setTotalPoints(initialPoints);
            points.setCreateTime(LocalDateTime.now());
            points.setUpdateTime(LocalDateTime.now());
            memberPointsMapper.insert(points);

            // 记录初始化流水
            saveTransaction(userId, PointsTransaction.TYPE_GIFT, initialPoints, 0,
                    initialPoints, "GIFT", null, "新用户赠送积分");

            log.info("=== 初始化用户积分记录！userId: {}, 赠送积分: {}", userId, initialPoints);
        }
        return points;
    }

    @Override
    @Transactional
    public void deductPoints(Integer points, String bizType) {
        Integer userId = getCurrentUserId();
        deductPoints(userId, points, bizType);
    }

    @Override
    @Transactional
    public void deductPoints(Integer userId, Integer points, String bizType) {
        if (points <= 0) {
            throw new CustomException("消耗积分数量必须大于0");
        }

        // 确保积分记录存在
        MemberPoints memberPoints = ensurePointsRecord(userId);
        Integer balanceBefore = memberPoints.getTotalPoints();

        // 扣减积分（SQL层带余额校验，防止并发超扣）
        int affectedRows = memberPointsMapper.deductPoints(userId, points);
        if (affectedRows == 0) {
            throw new CustomException(HttpStatus.PAYMENT_REQUIRED,
                    "积分余额不足，当前余额：" + balanceBefore + "，需要：" + points);
        }

        // 计算扣减后余额
        Integer balanceAfter = balanceBefore - points;

        // 记录流水
        saveTransaction(userId, PointsTransaction.TYPE_DEDUCTION, points,
                balanceBefore, balanceAfter, bizType, null,
                getBizDescription(bizType));

        log.info("=== 积分扣减成功！userId: {}, 扣减: {}, 余额: {} -> {}", userId, points, balanceBefore, balanceAfter);
    }

    @Override
    @Transactional
    public void rechargePoints(Integer userId, Integer points, String description) {
        if (points <= 0) {
            throw new CustomException("充值积分数量必须大于0");
        }

        MemberPoints memberPoints = ensurePointsRecord(userId);
        Integer balanceBefore = memberPoints.getTotalPoints();
        Integer balanceAfter = balanceBefore + points;

        // 更新积分
        MemberPoints updateEntity = new MemberPoints();
        updateEntity.setId(memberPoints.getId());
        updateEntity.setTotalPoints(balanceAfter);
        memberPointsMapper.updateById(updateEntity);

        // 记录流水
        saveTransaction(userId, PointsTransaction.TYPE_RECHARGE, points,
                balanceBefore, balanceAfter, null, null,
                description != null ? description : "积分充值");

        log.info("=== 积分充值成功！userId: {}, 充值: {}, 余额: {} -> {}", userId, points, balanceBefore, balanceAfter);
    }

    @Override
    @Transactional
    public void refundPoints(Integer points, String bizType, String reason) {
        Integer userId = getCurrentUserId();
        refundPoints(userId, points, bizType, reason);
    }

    @Override
    @Transactional
    public void refundPoints(Integer userId, Integer points, String bizType, String reason) {
        if (points <= 0) {
            throw new CustomException("退还积分数量必须大于0");
        }

        MemberPoints memberPoints = ensurePointsRecord(userId);
        Integer balanceBefore = memberPoints.getTotalPoints();
        Integer balanceAfter = balanceBefore + points;

        // 更新积分
        MemberPoints updateEntity = new MemberPoints();
        updateEntity.setId(memberPoints.getId());
        updateEntity.setTotalPoints(balanceAfter);
        memberPointsMapper.updateById(updateEntity);

        // 记录退款流水
        String description = (reason != null ? reason : "业务执行失败") + "（退还积分）";
        saveTransaction(userId, PointsTransaction.TYPE_REFUND, points,
                balanceBefore, balanceAfter, bizType, null, description);

        log.info("=== 积分退还成功！userId: {}, 退还: {}, 余额: {} -> {}, 原因: {}",
                userId, points, balanceBefore, balanceAfter, reason);
    }

    @Override
    @Transactional
    public void adminAdjustPoints(Integer userId, Integer points, String description) {
        if (points == null || points == 0) {
            throw new CustomException("调整积分数量不能为0");
        }

        MemberPoints memberPoints = ensurePointsRecord(userId);
        Integer balanceBefore = memberPoints.getTotalPoints();
        Integer balanceAfter = balanceBefore + points;

        if (balanceAfter < 0) {
            throw new CustomException("调整后积分不能为负数，当前余额：" + balanceBefore);
        }

        // 更新积分
        MemberPoints updateEntity = new MemberPoints();
        updateEntity.setId(memberPoints.getId());
        updateEntity.setTotalPoints(balanceAfter);
        memberPointsMapper.updateById(updateEntity);

        // 记录流水
        String type = points > 0 ? PointsTransaction.TYPE_ADMIN_ADJUST : PointsTransaction.TYPE_ADMIN_ADJUST;
        saveTransaction(userId, type, points, balanceBefore, balanceAfter, null, null,
                description != null ? description : "管理员调整积分");

        log.info("=== 管理员调整积分成功！userId: {}, 调整: {}, 余额: {} -> {}", userId, points, balanceBefore, balanceAfter);
    }

    @Override
    public PageVO<PointsTransaction> queryTransactions(Integer userId, String transactionType,
                                                        String bizType, Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<PointsTransaction> list = pointsTransactionMapper.queryPage(offset, pageSize, userId, transactionType, bizType);
        int total = pointsTransactionMapper.queryCount(userId, transactionType, bizType);

        PageVO<PointsTransaction> pageVO = new PageVO<>();
        pageVO.setList(list);
        pageVO.setTotal(total);
        return pageVO;
    }

    @Override
    public PageVO<MemberPointsVO> queryAllMemberPoints(String username, Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<MemberPointsVO> list = memberPointsMapper.selectAdminPage(offset, pageSize, username);
        int total = memberPointsMapper.selectAdminCount(username);

        PageVO<MemberPointsVO> pageVO = new PageVO<>();
        pageVO.setList(list);
        pageVO.setTotal(total);
        return pageVO;
    }

    @Override
    public PageVO<PointsTransaction> queryAllTransactions(String username, String transactionType,
                                                           String bizType, Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<PointsTransaction> list = pointsTransactionMapper.queryAllPage(offset, pageSize, username, transactionType, bizType);
        int total = pointsTransactionMapper.queryAllCount(username, transactionType, bizType);

        PageVO<PointsTransaction> pageVO = new PageVO<>();
        pageVO.setList(list);
        pageVO.setTotal(total);
        return pageVO;
    }

    // ==================== 私有方法 ====================

    /**
     * 保存积分流水记录
     */
    private void saveTransaction(Integer userId, String transactionType, Integer points,
                                  Integer balanceBefore, Integer balanceAfter,
                                  String bizType, String bizId, String description) {
        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setTransactionType(transactionType);
        transaction.setPoints(points);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setBizType(bizType);
        transaction.setBizId(bizId);
        transaction.setDescription(description);
        transaction.setCreateTime(LocalDateTime.now());
        pointsTransactionMapper.insert(transaction);
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        try {
            return CurrentUserThreadLocal.getCurrentUser().getId();
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
    }

    /**
     * 获取业务类型描述
     */
    private String getBizDescription(String bizType) {
        if (bizType == null) return "积分消耗";
        return switch (bizType) {
            case PointsTransaction.BIZ_COURSE -> "AI课程生成";
            case PointsTransaction.BIZ_PPT -> "PPT生成";
            case PointsTransaction.BIZ_MINDMAP -> "思维导图生成";
            case PointsTransaction.BIZ_CHAT -> "AI对话";
            case PointsTransaction.BIZ_EXAM -> "考试题目生成";
            case PointsTransaction.BIZ_VERIFY -> "内容真实性校验";
            default -> "积分消耗";
        };
    }
}
