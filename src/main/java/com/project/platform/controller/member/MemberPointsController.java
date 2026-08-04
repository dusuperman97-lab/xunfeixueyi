package com.project.platform.controller.member;

import com.project.platform.entity.MemberPoints;
import com.project.platform.entity.PointsTransaction;
import com.project.platform.service.MemberPointsService;
import com.project.platform.utils.CurrentUserThreadLocal;
import com.project.platform.vo.MemberPointsVO;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 会员积分管理
 */
@RestController
@RequestMapping("/member-points")
@Slf4j
public class MemberPointsController {

    @Resource
    private MemberPointsService memberPointsService;

    /**
     * 查询当前用户积分余额
     */
    @GetMapping("/balance")
    public ResponseVO<MemberPoints> getBalance() {
        Integer userId = getCurrentUserId();
        MemberPoints points = memberPointsService.ensurePointsRecord(userId);
        return ResponseVO.ok(points);
    }

    /**
     * 查询指定用户积分余额（管理员）
     */
    @GetMapping("/balance/{userId}")
    public ResponseVO<MemberPoints> getBalanceByUserId(@PathVariable Integer userId) {
        MemberPoints points = memberPointsService.getByUserId(userId);
        if (points == null) {
            return ResponseVO.fail(404, "用户积分记录不存在");
        }
        return ResponseVO.ok(points);
    }

    /**
     * 用户积分充值
     */
    @PostMapping("/recharge")
    public ResponseVO<MemberPoints> recharge(@RequestBody Map<String, Object> params) {
        Integer userId = getCurrentUserId();
        Integer points = (Integer) params.get("points");
        String description = (String) params.getOrDefault("description", "积分充值");

        if (points == null || points <= 0) {
            return ResponseVO.fail(400, "充值积分数量必须大于0");
        }

        memberPointsService.rechargePoints(userId, points, description);
        MemberPoints updated = memberPointsService.getByUserId(userId);
        return ResponseVO.ok(updated);
    }

    /**
     * 管理员调整用户积分（可增可减）
     */
    @PostMapping("/admin-adjust")
    public ResponseVO<MemberPoints> adminAdjust(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer points = (Integer) params.get("points");
        String description = (String) params.getOrDefault("description", "管理员调整");

        if (userId == null) {
            return ResponseVO.fail(400, "用户ID不能为空");
        }
        if (points == null || points == 0) {
            return ResponseVO.fail(400, "调整积分数量不能为0");
        }

        memberPointsService.adminAdjustPoints(userId, points, description);
        MemberPoints updated = memberPointsService.getByUserId(userId);
        return ResponseVO.ok(updated);
    }

    /**
     * 分页查询积分流水
     *
     * @param query   查询条件（可选：transactionType, bizType）
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     */
    @GetMapping("/transactions")
    public ResponseVO<PageVO<PointsTransaction>> queryTransactions(
            @RequestParam Map<String, Object> query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer userId = getCurrentUserId();
        String transactionType = (String) query.get("transactionType");
        String bizType = (String) query.get("bizType");

        PageVO<PointsTransaction> pageVO = memberPointsService.queryTransactions(
                userId, transactionType, bizType, pageNum, pageSize);
        return ResponseVO.ok(pageVO);
    }

    /**
     * 管理员分页查询指定用户的积分流水
     */
    @GetMapping("/transactions/{userId}")
    public ResponseVO<PageVO<PointsTransaction>> queryTransactionsByUserId(
            @PathVariable Integer userId,
            @RequestParam Map<String, Object> query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        String transactionType = (String) query.get("transactionType");
        String bizType = (String) query.get("bizType");

        PageVO<PointsTransaction> pageVO = memberPointsService.queryTransactions(
                userId, transactionType, bizType, pageNum, pageSize);
        return ResponseVO.ok(pageVO);
    }

    private Integer getCurrentUserId() {
        return CurrentUserThreadLocal.getCurrentUser().getId();
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端：分页查询所有用户积分（含用户名）
     */
    @GetMapping("/admin/list")
    public ResponseVO<PageVO<MemberPointsVO>> adminList(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageVO<MemberPointsVO> pageVO = memberPointsService.queryAllMemberPoints(username, pageNum, pageSize);
        return ResponseVO.ok(pageVO);
    }

    /**
     * 管理端：分页查询所有积分流水（不限用户）
     */
    @GetMapping("/admin/transactions")
    public ResponseVO<PageVO<PointsTransaction>> adminTransactions(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageVO<PointsTransaction> pageVO = memberPointsService.queryAllTransactions(
                username, transactionType, bizType, pageNum, pageSize);
        return ResponseVO.ok(pageVO);
    }
}
