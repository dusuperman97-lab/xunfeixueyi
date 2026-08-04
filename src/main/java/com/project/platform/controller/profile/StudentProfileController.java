package com.project.platform.controller.profile;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.StudentProfile;
import com.project.platform.service.StudentProfileService;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.StudentProfileDashboardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@Slf4j
public class StudentProfileController {

    @Autowired
    private Tools tools;

    @Autowired
    private StudentProfileService studentProfileService;

    /**
     * 获取当前学生的画像信息
     */
    @GetMapping("/get-student-profile")
    public ResponseVO<StudentProfile> getStudentProfile() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 查询学生画像，studentId: {}", studentId);

        StudentProfile profile = studentProfileService.getProfileByStudentId(studentId);
        if (profile == null) {
            return ResponseVO.fail(404, "暂无画像信息，请先与AI助手对话");
        }
        return ResponseVO.ok(profile);
    }

    /**
     * 获取数据大屏仪表盘数据
     * 返回按模块分组的大屏综合数据，格式直接匹配前端图表组件
     */
    @GetMapping("/dashboard")
    public ResponseVO<StudentProfileDashboardVO> getDashboard() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 查询数据大屏数据，studentId: {}", studentId);

        StudentProfileDashboardVO vo = studentProfileService.getDashboard(studentId);
        return ResponseVO.ok(vo);
    }

    /**
     * 删除学生画像
     */
    @DeleteMapping("/delete-student-profile")
    public ResponseVO<Void> deleteStudentProfile() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 删除学生画像，studentId: {}", studentId);

        studentProfileService.deleteProfile(studentId);
        return ResponseVO.ok(null);
    }
}
