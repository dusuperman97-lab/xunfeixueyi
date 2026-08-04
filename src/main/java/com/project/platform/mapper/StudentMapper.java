package com.project.platform.mapper;

import com.project.platform.entity.Student;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface StudentMapper {

    List<Student> queryPage(Integer offset, Integer pageSize, @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    @Select("SELECT * FROM student WHERE id = #{id}")
    Student selectById(Integer id);

    @Select("SELECT * FROM student")
    List<Student> list();

    int insert(Student entity);

    int updateById(Student entity);

    boolean removeByIds(List<Integer> ids);

    @Select("SELECT * FROM student WHERE username = #{username}")
    Student selectByUserName(String username);

    @Select("SELECT * FROM student WHERE student_no = #{studentNo}")
    Student selectByStudentNo(String studentNo);

    @Select("SELECT COUNT(*) FROM student WHERE DATE(create_time) = CURDATE()")
    int countTodayNewUsers();

    @Select("SELECT COUNT(*) FROM student WHERE DATE(create_time) = #{date}")
    int countUsersByDate(@Param("date") String date);

    @Select("SELECT id, student_no AS studentNo, username, avatar_url AS avatarUrl, create_time AS createTime FROM student ORDER BY create_time DESC LIMIT #{limit}")
    List<Student> selectRecentUsers(@Param("limit") int limit);
}