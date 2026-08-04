package com.project.platform.mapper;

import com.project.platform.entity.MemberPoints;
import com.project.platform.vo.MemberPointsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户积分 Mapper
 */
public interface MemberPointsMapper {

    @Select("SELECT id, user_id, total_points, create_time, update_time FROM member_points WHERE user_id = #{userId}")
    MemberPoints selectByUserId(Integer userId);

    int insert(MemberPoints entity);

    int updateById(MemberPoints entity);

    /**
     * 扣减积分（带行锁，防止并发超扣）
     */
    int deductPoints(@Param("userId") Integer userId, @Param("points") Integer points);

    /**
     * 管理端：分页查询所有用户积分（含用户名）
     */
    List<MemberPointsVO> selectAdminPage(@Param("offset") Integer offset,
                                         @Param("pageSize") Integer pageSize,
                                         @Param("username") String username);

    /**
     * 管理端：查询所有用户积分总数
     */
    int selectAdminCount(@Param("username") String username);
}
