package com.project.platform.service;

import com.project.platform.entity.Student;
import com.project.platform.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学生信息表 服务类
 * </p>
 */
public interface StudentService extends CommonService<Student> {

    PageVO<Student> page(Map<String, Object> query, Integer pageNum, Integer pageSize);

    List<Student> list();

    void insert(Student entity);

    void updateById(Student entity);

    void removeByIds(List<Integer> ids);
}