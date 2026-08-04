package com.project.platform.mapper;

import com.project.platform.entity.PointsTransaction;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 积分流水 Mapper
 */
public interface PointsTransactionMapper {

    int insert(PointsTransaction entity);

    List<PointsTransaction> queryPage(@Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize,
                                       @Param("userId") Integer userId,
                                       @Param("transactionType") String transactionType,
                                       @Param("bizType") String bizType);

    int queryCount(@Param("userId") Integer userId,
                   @Param("transactionType") String transactionType,
                   @Param("bizType") String bizType);

    /**
     * 管理端：分页查询所有积分流水（不限用户，可选用户名筛选）
     */
    List<PointsTransaction> queryAllPage(@Param("offset") Integer offset,
                                         @Param("pageSize") Integer pageSize,
                                         @Param("username") String username,
                                         @Param("transactionType") String transactionType,
                                         @Param("bizType") String bizType);

    /**
     * 管理端：查询所有积分流水总数
     */
    int queryAllCount(@Param("username") String username,
                      @Param("transactionType") String transactionType,
                      @Param("bizType") String bizType);
}
