package com.project.platform.mapper;

import com.project.platform.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学生画像Mapper接口
 */
@Mapper
public interface StudentProfileMapper {

    /**
     * 根据学生ID查询画像
     */
    @Select("SELECT * FROM student_profile WHERE student_id = #{studentId}")
    StudentProfile selectByStudentId(Integer studentId);

    /**
     * 插入画像记录
     */
    int insert(StudentProfile profile);

    /**
     * 更新画像记录
     */
    int updateById(StudentProfile profile);

    /**
     * 根据学生ID删除画像
     */
    int deleteByStudentId(@Param("studentId") Integer studentId);
}
