package com.project.platform.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.project.platform.dto.CurrentUserDTO;
import com.project.platform.dto.RetrievePasswordDTO;
import com.project.platform.dto.UpdatePasswordDTO;
import com.project.platform.entity.Student;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.StudentMapper;
import com.project.platform.service.StudentService;
import com.project.platform.utils.CurrentUserThreadLocal;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StudentServiceImpl implements StudentService {

    @Resource
    private StudentMapper studentMapper;

    @Override
    public PageVO<Student> page(Map<String, Object> query, Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<Student> list = studentMapper.queryPage(offset, pageSize, query);
        int total = studentMapper.queryCount(query);

        PageVO<Student> pageVO = new PageVO<>();
        pageVO.setList(list);
        pageVO.setTotal(total);
        return pageVO;
    }

    @Override
    public List<Student> list() {
        return studentMapper.list();
    }

    @Override
    public void insert(Student entity) {
        // 校验学号或用户名是否已存在
        if (studentMapper.selectByStudentNo(entity.getStudentNo()) != null) {
            throw new CustomException("学号已存在");
        }
        if (studentMapper.selectByUserName(entity.getUsername()) != null) {
            throw new CustomException("用户名已存在");
        }

        // 新增：如果没有传递状态，默认设置为正常 ("1")
        if (entity.getStatus() == null) {
            entity.setStatus("1");
        }

        // 新增：注册完成后，设置 isRegistered 为 false（表示新用户未完成画像引导）
        if (entity.getIsRegistered() == null) {
            entity.setIsRegistered(false);
        }

        studentMapper.insert(entity);
    }

    @Override
    public void updateById(Student entity) {
        studentMapper.updateById(entity);
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        studentMapper.removeByIds(ids);
    }

    @Override
    public CurrentUserDTO login(String username, String password) {
        Student student = studentMapper.selectByUserName(username);
        if (student == null) {
            // 支持学号登录
            student = studentMapper.selectByStudentNo(username);
        }

        if (student == null) {
            throw new CustomException("用户不存在");
        }

        // 新增：账号状态校验 (假设 "0" 代表禁用)
        if ("0".equals(student.getStatus())) {
            throw new CustomException("该账号已被禁用，请联系管理员");
        }

        if (!student.getPassword().equals(password)) {
            throw new CustomException("密码错误");
        }

        // 封装返回给 Controller 的 DTO
        CurrentUserDTO currentUserDTO = new CurrentUserDTO();
        currentUserDTO.setId(student.getId());
        currentUserDTO.setUsername(student.getUsername());
        currentUserDTO.setNickname(student.getUsername()); // 学生表没有昵称，暂用用户名替代
        currentUserDTO.setAvatarUrl(student.getAvatarUrl());
        currentUserDTO.setIsRegistered(student.getIsRegistered());
        return currentUserDTO;
    }

    @Override
    public void register(JSONObject data) {
        Student student = data.to(Student.class);
        if (student.getPassword() == null) {
            throw new CustomException("密码不能为空");
        }
        this.insert(student); // 复用校验和插入逻辑
    }

    @Override
    public void updateCurrentUserInfo(CurrentUserDTO currentUserDTO) {
        Student updateEntity = new Student();
        updateEntity.setId(currentUserDTO.getId());
        updateEntity.setUsername(currentUserDTO.getUsername());
        updateEntity.setAvatarUrl(currentUserDTO.getAvatarUrl());
        updateEntity.setTel(currentUserDTO.getTel());
        updateEntity.setEmail(currentUserDTO.getEmail());
        // 执行更新
        studentMapper.updateById(updateEntity);
    }

    @Override
    public void updateCurrentUserPassword(UpdatePasswordDTO updatePassword) {
        Integer currentUserId = CurrentUserThreadLocal.getCurrentUser().getId();
        Student student = studentMapper.selectById(currentUserId);

        if (student == null || !student.getPassword().equals(updatePassword.getOldPassword())) {
            throw new CustomException("原密码错误");
        }

        Student updateEntity = new Student();
        updateEntity.setId(currentUserId);
        updateEntity.setPassword(updatePassword.getNewPassword());
        studentMapper.updateById(updateEntity);
    }

    @Override
    public void resetPassword(Integer id) {
        Student updateEntity = new Student();
        updateEntity.setId(id);
        updateEntity.setPassword("123456"); // 系统默认重置密码
        studentMapper.updateById(updateEntity);
    }

    @Override
    public void retrievePassword(RetrievePasswordDTO retrievePasswordDTO) {
        Student student = studentMapper.selectByStudentNo(retrievePasswordDTO.getTel());
        if (student == null) {
            throw new CustomException("学号不存在，无法找回密码");
        }


        Student updateEntity = new Student();
        updateEntity.setId(student.getId());
        updateEntity.setPassword(retrievePasswordDTO.getPassword());
        studentMapper.updateById(updateEntity);
    }

    @Override
    public Student selectById(Integer id) {
        return studentMapper.selectById(id);
    }

}