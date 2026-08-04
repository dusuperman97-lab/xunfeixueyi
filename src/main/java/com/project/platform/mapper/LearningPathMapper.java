package com.project.platform.mapper;

import com.project.platform.entity.LearningPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习路线Mapper接口
 */
@Mapper
public interface LearningPathMapper {

    /**
     * 根据用户ID和路线类型查询
     */
    @Select("SELECT * FROM learning_path WHERE user_id = #{userId} AND path_type = #{pathType} ORDER BY create_time DESC LIMIT 1")
    LearningPath selectByUserIdAndType(@Param("userId") Integer userId, @Param("pathType") String pathType);

    /**
     * 查询用户所有学习路线
     */
    @Select("SELECT * FROM learning_path WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<LearningPath> selectByUserId(Integer userId);

    /**
     * 插入学习路线
     */
    int insert(LearningPath learningPath);

    /**
     * 更新学习路线
     */
    int updateById(LearningPath learningPath);

    /**
     * 根据ID删除
     */
    int deleteById(Integer id);
}
