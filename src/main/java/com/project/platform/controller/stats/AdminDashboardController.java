package com.project.platform.controller.stats;

import com.project.platform.entity.Student;
import com.project.platform.mapper.AiToolUsageMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.DigitalHumanMapper;
import com.project.platform.mapper.QuestionMapper;
import com.project.platform.mapper.StudentMapper;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制台统计
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private DigitalHumanMapper digitalHumanMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private AiToolUsageMapper aiToolUsageMapper;

    /**
     * 获取控制台统计数据
     */
    @GetMapping("/stats")
    public ResponseVO<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();

        // 基础统计
        result.put("userCount", studentMapper.queryCount(new HashMap<>()));
        result.put("digitalHumanCount", digitalHumanMapper.queryCount(new HashMap<>()));
        result.put("courseCount", courseMapper.queryCount(new HashMap<>()));
        result.put("questionCount", questionMapper.queryAllCount());

        // 今日新增用户
        result.put("todayNewUsers", studentMapper.countTodayNewUsers());

        // AI工具总使用次数
        result.put("totalAiUsage", aiToolUsageMapper.sumTotalUsageCount());

        // 活跃用户数（使用过AI工具的用户）
        result.put("activeUserCount", aiToolUsageMapper.countActiveUsers());

        // 近7天新增用户趋势
        List<Integer> weeklyNewUsers = new ArrayList<>();
        List<String> weeklyDates = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            weeklyDates.add(date.format(fmt));
            weeklyNewUsers.add(studentMapper.countUsersByDate(date.toString()));
        }
        result.put("weeklyNewUsers", weeklyNewUsers);
        result.put("weeklyNewUsersDates", weeklyDates);

        // 最近注册的5个用户
        List<Student> recentStudents = studentMapper.selectRecentUsers(5);
        List<Map<String, Object>> recentUsers = recentStudents.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("username", s.getUsername());
            m.put("avatarUrl", s.getAvatarUrl());
            m.put("createTime", s.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        result.put("recentUsers", recentUsers);

        return ResponseVO.ok(result);
    }
}
