package com.project.platform.service.impl;

import com.project.platform.dto.CourseStudyStatsDTO;
import com.project.platform.entity.Student;
import com.project.platform.entity.StudyDurationRecord;
import com.project.platform.mapper.StudyDurationRecordMapper;
import com.project.platform.service.StudentService;
import com.project.platform.service.StudyDurationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 学生学习时长统计Service实现
 */
@Slf4j
@Service
public class StudyDurationServiceImpl implements StudyDurationService {

    @Resource
    private StudyDurationRecordMapper studyDurationRecordMapper;

    @Resource
    private StudentService studentService;

    @Override
    public Student getStudyDurationStats(Integer studentId, Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }

        log.info("=== 查询学生学习时长统计，studentId: {}, days: {}", studentId, days);

        Student student = studentService.selectById(studentId);

        Map<String, Integer> dailyMap = buildDailyMap(days);
        List<Map<String, Object>> statsList = studyDurationRecordMapper.getStudyDurationStats(studentId, days);
        mergeDailyData(dailyMap, statsList);

        List<String> dates = new ArrayList<>(dailyMap.keySet());
        List<Integer> durations = new ArrayList<>(dailyMap.values());

        Integer totalMinutes = studyDurationRecordMapper.getTotalStudyDuration(studentId, days);
        if (totalMinutes == null) totalMinutes = 0;

        Integer avgMinutes = totalMinutes / days;

        student.setStudyDurationDates(dates);
        student.setStudyDurationMinutes(durations);
        student.setTotalStudyMinutes(totalMinutes);
        student.setAvgStudyMinutes(avgMinutes);

        log.info("=== 学习时长统计结果：总时长={}分钟，平均时长={}分钟/天", totalMinutes, avgMinutes);

        return student;
    }

    @Override
    public CourseStudyStatsDTO getCourseStudyDurationStats(Integer studentId, Integer courseId, Integer days) {
        if (days == null || days <= 0) days = 7;

        log.info("=== 查询学生课程学习时长统计，studentId: {}, courseId: {}, days: {}", studentId, courseId, days);

        CourseStudyStatsDTO dto = new CourseStudyStatsDTO();
        dto.setCourseId(courseId);

        Map<String, Integer> dailyMap = buildDailyMap(days);
        List<Map<String, Object>> statsList = studyDurationRecordMapper.getStudyDurationStatsByCourse(studentId, courseId, days);
        mergeDailyData(dailyMap, statsList);

        dto.setDates(new ArrayList<>(dailyMap.keySet()));
        dto.setDurations(new ArrayList<>(dailyMap.values()));

        Integer totalMinutes = studyDurationRecordMapper.getTotalStudyDurationByCourse(studentId, courseId, days);
        if (totalMinutes == null) totalMinutes = 0;
        dto.setTotalMinutes(totalMinutes);
        dto.setAvgMinutes(totalMinutes / days);

        log.info("=== 课程学习时长统计：courseId={}, 总时长={}分钟", courseId, totalMinutes);
        return dto;
    }

    @Override
    public List<CourseStudyStatsDTO> getStudyDurationGroupByCourse(Integer studentId, Integer days) {
        if (days == null || days <= 0) days = 7;

        log.info("=== 查询学生各课程学习时长分布，studentId: {}, days: {}", studentId, days);

        List<Map<String, Object>> groupList = studyDurationRecordMapper.getStudyDurationGroupByCourse(studentId, days);
        List<CourseStudyStatsDTO> result = new ArrayList<>();

        if (groupList != null && !groupList.isEmpty()) {
            for (Map<String, Object> row : groupList) {
                Object courseIdObj = row.get("courseId");
                Object totalObj = row.get("totalMinutes");
                // 跳过旧数据（course_id 为 NULL 的记录）
                if (courseIdObj == null || totalObj == null) continue;
                CourseStudyStatsDTO dto = new CourseStudyStatsDTO();
                dto.setCourseId(((Number) courseIdObj).intValue());
                dto.setCourseName((String) row.get("courseName"));
                dto.setTotalMinutes(((Number) totalObj).intValue());
                dto.setAvgMinutes(dto.getTotalMinutes() / days);
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    public void recordStudyDuration(Integer studentId, Integer courseId, Integer durationMinutes,
                                    String studyType, Integer sectionId) {
        if (studentId == null || durationMinutes == null || durationMinutes <= 0) {
            log.warn("=== 记录学习时长参数无效：studentId={}, durationMinutes={}", studentId, durationMinutes);
            return;
        }

        LocalDate today = LocalDate.now();

        // 按（学生 + 课程 + 日期）合并记录
        StudyDurationRecord existingRecord;
        if (courseId != null) {
            existingRecord = studyDurationRecordMapper.selectByStudentCourseAndDate(studentId, courseId, today);
        } else {
            existingRecord = studyDurationRecordMapper.selectByStudentAndDate(studentId, today);
        }

        if (existingRecord != null) {
            existingRecord.setDurationMinutes(existingRecord.getDurationMinutes() + durationMinutes);
            if (studyType != null) existingRecord.setStudyType(studyType);
            if (sectionId != null) existingRecord.setSectionId(sectionId);
            studyDurationRecordMapper.updateById(existingRecord);
            log.info("=== 更新学习时长记录成功，studentId: {}, courseId: {}, 累计时长: {}分钟",
                    studentId, courseId, existingRecord.getDurationMinutes());
        } else {
            StudyDurationRecord record = new StudyDurationRecord();
            record.setStudentId(studentId);
            record.setCourseId(courseId);
            record.setStudyDate(today);
            record.setDurationMinutes(durationMinutes);
            record.setStudyType(studyType);
            record.setSectionId(sectionId);
            studyDurationRecordMapper.insert(record);
            log.info("=== 新增学习时长记录成功，studentId: {}, courseId: {}, 时长: {}分钟",
                    studentId, courseId, durationMinutes);
        }
    }

    @Override
    public List<Student> getStudentList() {
        List<Student> list = studentService.list();
        int days = 7;
        for (Student student : list) {
            Map<String, Integer> dailyMap = buildDailyMap(days);
            List<Map<String, Object>> statsList = studyDurationRecordMapper.getStudyDurationStats(student.getId(), days);
            mergeDailyData(dailyMap, statsList);

            List<String> dates = new ArrayList<>(dailyMap.keySet());
            List<Integer> durations = new ArrayList<>(dailyMap.values());

            Integer totalMinutes = studyDurationRecordMapper.getTotalStudyDuration(student.getId(), days);
            if (totalMinutes == null) totalMinutes = 0;

            student.setStudyDurationDates(dates);
            student.setStudyDurationMinutes(durations);
            student.setTotalStudyMinutes(totalMinutes);
            student.setAvgStudyMinutes(totalMinutes / days);
        }
        return list;
    }

    // ==================== 私有工具方法 ====================

    private Map<String, Integer> buildDailyMap(int days) {
        Map<String, Integer> dailyMap = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            dailyMap.put(LocalDate.now().minusDays(i).toString(), 0);
        }
        return dailyMap;
    }

    private void mergeDailyData(Map<String, Integer> dailyMap, List<Map<String, Object>> statsList) {
        if (statsList != null && !statsList.isEmpty()) {
            for (Map<String, Object> stat : statsList) {
                String date = (String) stat.get("date");
                int duration = ((Number) stat.get("duration")).intValue();
                dailyMap.put(date, duration);
            }
        }
    }
}
