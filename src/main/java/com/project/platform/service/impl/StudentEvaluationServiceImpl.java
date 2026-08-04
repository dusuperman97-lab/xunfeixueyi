package com.project.platform.service.impl;

import com.project.platform.entity.*;
import com.project.platform.mapper.*;
import com.project.platform.service.StudentEvaluationService;
import com.project.platform.vo.StudentEvaluationVO;
import com.project.platform.vo.StudentEvaluationVO.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudentEvaluationServiceImpl implements StudentEvaluationService {

    @Autowired
    private AiToolUsageMapper aiToolUsageMapper;

    @Autowired
    private SectionTestSummaryMapper summaryMapper;

    @Autowired
    private KnowledgePointStatsMapper knowledgePointStatsMapper;

    @Autowired
    private StudyDurationRecordMapper studyDurationRecordMapper;

    @Autowired
    private QaHistoryMapper qaHistoryMapper;

    @Autowired
    private SavedResourceMapper savedResourceMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private SectionMapper sectionMapper;

    @Override
    public StudentEvaluationVO getDashboard(Integer userId, Integer courseId, Integer days) {
        if (days == null || days <= 0) days = 30;

        StudentEvaluationVO vo = new StudentEvaluationVO();
        vo.setOverview(buildOverview(userId, courseId, days));
        vo.setToolUsage(buildToolUsage(userId));
        vo.setTestTrend(buildTestTrend(userId, courseId, days));
        vo.setKnowledgeMastery(buildKnowledgeMastery(userId));
        vo.setDailyStudy(buildDailyStudy(userId, courseId, days));
        vo.setQaFeedback(buildQaFeedback(userId, days));
        vo.setHourActivity(buildHourActivity(userId, days));
        vo.setCourseProgress(buildCourseProgress(userId));
        vo.setCourseStudyTime(buildCourseStudyTime(userId, courseId, days));
        return vo;
    }

    private OverviewStats buildOverview(Integer userId, Integer courseId, int days) {
        OverviewStats stats = new OverviewStats();

        // 总学习时长（分钟）
        Integer totalMinutes = studyDurationRecordMapper.getTotalStudyDuration(userId, days);
        stats.setTotalStudyMinutes(totalMinutes != null ? totalMinutes : 0);

        // 测试次数 & 平均正确率
        List<SectionTestSummary> allTests = summaryMapper.selectCompletedByUser(userId);
        List<SectionTestSummary> filteredTests;
        if (courseId != null) {
            filteredTests = allTests.stream()
                    .filter(t -> courseId.equals(t.getCourseId()))
                    .collect(Collectors.toList());
        } else {
            filteredTests = allTests;
        }
        stats.setTotalTestCount(filteredTests.size());
        if (!filteredTests.isEmpty()) {
            double avgAcc = filteredTests.stream()
                    .map(t -> t.getAccuracyRate() != null ? t.getAccuracyRate() : BigDecimal.ZERO)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0);
            stats.setAvgAccuracy(Math.round(avgAcc * 10.0) / 10.0);
        } else {
            stats.setAvgAccuracy(0.0);
        }

        // AI 工具使用记录（一次查询，多处使用）
        AiToolUsage usage = aiToolUsageMapper.selectByUserId(userId);

        // AI 问答总次数 = 大模型对话次数 + 知识库问答次数
        int chatCount = (usage != null && usage.getChatUsageCount() != null) ? usage.getChatUsageCount() : 0;
        int kbQaCount = qaHistoryMapper.countByStudentIdAndDays(userId.longValue(), days);
        stats.setTotalQaCount(chatCount + kbQaCount);

        // 资源保存数
        stats.setTotalResourcesSaved(savedResourceMapper.countByUserId(userId.longValue()));

        // 最大连续学习天数
        stats.setMaxConsecutiveDays(calcMaxConsecutiveDays(userId, courseId, days));

        // 投入度评分（来自 AI 工具使用分析）
        if (usage != null && usage.getEngagementScore() != null) {
            stats.setEngagementScore(usage.getEngagementScore().intValue());
        } else {
            stats.setEngagementScore(calcSimpleEngagement(userId, days));
        }

        return stats;
    }

    private List<ToolUsageStat> buildToolUsage(Integer userId) {
        AiToolUsage u = aiToolUsageMapper.selectByUserId(userId);
        List<ToolUsageStat> list = new ArrayList<>();

        list.add(makeToolStat("chat", "大模型对话", "ChatDotRound",
                u == null ? 0 : nvl(u.getChatUsageCount()),
                u == null ? 0 : nvl(u.getChatDurationMinutes())));
        list.add(makeToolStat("ppt", "PPT生成", "Document",
                u == null ? 0 : nvl(u.getPptUsageCount()),
                u == null ? 0 : nvl(u.getPptDurationMinutes())));
        list.add(makeToolStat("mindmap", "思维导图", "Share",
                u == null ? 0 : nvl(u.getMindmapUsageCount()),
                u == null ? 0 : nvl(u.getMindmapDurationMinutes())));
        list.add(makeToolStat("resource", "资源推荐", "Search",
                u == null ? 0 : nvl(u.getResourceRecommendUsageCount()),
                u == null ? 0 : nvl(u.getResourceDurationMinutes())));
        list.add(makeToolStat("course", "沉浸式课程", "VideoPlay",
                u == null ? 0 : nvl(u.getImmersiveCourseUsageCount()),
                u == null ? 0 : nvl(u.getCourseDurationMinutes())));
        list.add(makeToolStat("teacher", "虚拟教师", "UserFilled",
                u == null ? 0 : nvl(u.getVirtualTeacherUsageCount()),
                u == null ? 0 : nvl(u.getTeacherDurationMinutes())));
        return list;
    }

    private ToolUsageStat makeToolStat(String key, String label, String icon, int count, int duration) {
        ToolUsageStat s = new ToolUsageStat();
        s.setToolKey(key);
        s.setToolLabel(label);
        s.setIcon(icon);
        s.setUsageCount(count);
        s.setDurationMinutes(duration);
        return s;
    }

    private int nvl(Integer v) { return v == null ? 0 : v; }

    private List<TestTrendPoint> buildTestTrend(Integer userId, Integer courseId, int days) {
        List<SectionTestSummary> allTests = summaryMapper.selectCompletedByUser(userId);
        LocalDate cutoff = LocalDate.now().minusDays(days);

        // 过滤 courseId 和时间范围
        List<SectionTestSummary> filtered = allTests.stream()
                .filter(t -> courseId == null || courseId.equals(t.getCourseId()))
                .filter(t -> t.getStartTime() != null && t.getStartTime().toLocalDate().isAfter(cutoff.minusDays(1)))
                .collect(Collectors.toList());

        // 按日期分组
        Map<LocalDate, List<SectionTestSummary>> byDate = filtered.stream()
                .collect(Collectors.groupingBy(t -> t.getStartTime().toLocalDate(),
                        TreeMap::new, Collectors.toList()));

        List<TestTrendPoint> points = new ArrayList<>();
        for (Map.Entry<LocalDate, List<SectionTestSummary>> e : byDate.entrySet()) {
            TestTrendPoint p = new TestTrendPoint();
            p.setDate(e.getKey().toString());
            p.setTestCount(e.getValue().size());
            double avgAcc = e.getValue().stream()
                    .map(t -> t.getAccuracyRate() != null ? t.getAccuracyRate() : BigDecimal.ZERO)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0);
            p.setAvgAccuracy(Math.round(avgAcc * 10.0) / 10.0);
            double avgDur = e.getValue().stream()
                    .mapToInt(t -> t.getTotalTimeSpent() != null ? t.getTotalTimeSpent() : 0)
                    .average().orElse(0);
            p.setAvgDuration((int) avgDur);
            points.add(p);
        }
        return points;
    }

    private List<KnowledgeMastery> buildKnowledgeMastery(Integer userId) {
        List<KnowledgePointStats> statsList = knowledgePointStatsMapper.selectByUserId(userId);
        if (statsList == null || statsList.isEmpty()) return new ArrayList<>();

        // 按知识点名称聚合（同一知识点可能分散在不同小节中）
        Map<String, List<KnowledgePointStats>> grouped = statsList.stream()
                .filter(k -> k.getTotalCount() != null && k.getTotalCount() > 0)
                .filter(k -> k.getKnowledgePoint() != null && !k.getKnowledgePoint().isBlank())
                .collect(Collectors.groupingBy(KnowledgePointStats::getKnowledgePoint));

        return grouped.entrySet().stream()
                .map(e -> {
                    int totalCnt = e.getValue().stream().mapToInt(KnowledgePointStats::getTotalCount).sum();
                    int correctCnt = e.getValue().stream().mapToInt(k -> k.getCorrectCount() != null ? k.getCorrectCount() : 0).sum();
                    double avgAcc = totalCnt > 0 ? (double) correctCnt / totalCnt * 100.0 : 0;
                    String level = avgAcc >= 85 ? "MASTER" : avgAcc >= 70 ? "ADVANCED" : avgAcc >= 50 ? "INTERMEDIATE" : "BEGINNER";

                    KnowledgeMastery m = new KnowledgeMastery();
                    m.setKnowledgePoint(e.getKey());
                    m.setAccuracyRate(BigDecimal.valueOf(Math.round(avgAcc * 10.0) / 10.0));
                    m.setMasteryLevel(level);
                    m.setTotalCount(totalCnt);
                    return m;
                })
                .sorted((a, b) -> {
                    double accA = a.getAccuracyRate() != null ? a.getAccuracyRate().doubleValue() : 0;
                    double accB = b.getAccuracyRate() != null ? b.getAccuracyRate().doubleValue() : 0;
                    return Double.compare(accA, accB);
                })
                .collect(Collectors.toList());
    }

    private List<DailyStudy> buildDailyStudy(Integer userId, Integer courseId, int days) {
        List<Map<String, Object>> raw;
        if (courseId != null) {
            raw = studyDurationRecordMapper.getStudyDurationStatsByCourse(userId, courseId, days);
        } else {
            raw = studyDurationRecordMapper.getStudyDurationStats(userId, days);
        }

        if (raw == null) return new ArrayList<>();
        return raw.stream().map(m -> {
            DailyStudy d = new DailyStudy();
            d.setDate(String.valueOf(m.get("date")));
            Object dur = m.get("duration");
            d.setDurationMinutes(dur instanceof Number ? ((Number) dur).intValue() : 0);
            return d;
        }).collect(Collectors.toList());
    }

    private QaFeedback buildQaFeedback(Integer userId, int days) {
        QaFeedback fb = new QaFeedback();
        List<Map<String, Object>> rows = qaHistoryMapper.selectFeedbackSummaryByDays(userId.longValue(), days);

        int up = 0, down = 0, none = 0;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object key = row.get("feedback");
                Object val = row.get("cnt");
                int c = val instanceof Number ? ((Number) val).intValue() : 0;
                if (key == null) { none += c; }
                else {
                    int k = key instanceof Number ? ((Number) key).intValue() : 0;
                    if (k == 1) up += c;
                    else if (k == -1) down += c;
                    else none += c;
                }
            }
        }
        fb.setThumbsUp(up);
        fb.setThumbsDown(down);
        fb.setNoFeedback(none);
        fb.setTotal(up + down + none);
        return fb;
    }

    private List<HourActivity> buildHourActivity(Integer userId, int days) {
        // 合并来自测试和问答的小时分布
        Map<Integer, int[]> hourMap = new TreeMap<>();
        for (int h = 0; h < 24; h++) hourMap.put(h, new int[]{0, 0});

        List<Map<String, Object>> testHours = summaryMapper.selectHourDistributionByUser(userId);
        if (testHours != null) {
            for (Map<String, Object> m : testHours) {
                Object hObj = m.get("hour");
                Object cObj = m.get("cnt");
                int h = hObj instanceof Number ? ((Number) hObj).intValue() : -1;
                int c = cObj instanceof Number ? ((Number) cObj).intValue() : 0;
                if (h >= 0 && h < 24) hourMap.get(h)[0] += c;
            }
        }

        List<Map<String, Object>> qaHours = qaHistoryMapper.selectHourDistributionByDays(userId.longValue(), days);
        if (qaHours != null) {
            for (Map<String, Object> m : qaHours) {
                Object hObj = m.get("hour");
                Object cObj = m.get("cnt");
                int h = hObj instanceof Number ? ((Number) hObj).intValue() : -1;
                int c = cObj instanceof Number ? ((Number) cObj).intValue() : 0;
                if (h >= 0 && h < 24) hourMap.get(h)[1] += c;
            }
        }

        List<HourActivity> result = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : hourMap.entrySet()) {
            HourActivity ha = new HourActivity();
            ha.setHour(e.getKey());
            ha.setSessionCount(e.getValue()[0] + e.getValue()[1]);
            ha.setTotalMinutes(e.getValue()[0] * 10 + e.getValue()[1] * 2); // 粗略估计
            result.add(ha);
        }
        return result;
    }

    private int calcMaxConsecutiveDays(Integer userId, Integer courseId, int days) {
        List<Map<String, Object>> raw;
        if (courseId != null) {
            raw = studyDurationRecordMapper.getStudyDurationStatsByCourse(userId, courseId, days);
        } else {
            raw = studyDurationRecordMapper.getStudyDurationStats(userId, days);
        }
        if (raw == null || raw.isEmpty()) return 0;

        Set<LocalDate> activeDates = new HashSet<>();
        for (Map<String, Object> m : raw) {
            String dateStr = String.valueOf(m.get("date"));
            Object dur = m.get("duration");
            int d = dur instanceof Number ? ((Number) dur).intValue() : 0;
            if (d > 0) {
                try { activeDates.add(LocalDate.parse(dateStr)); } catch (Exception ignored) {}
            }
        }

        int maxConsecutive = 0, current = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= days; i++) {
            if (activeDates.contains(today.minusDays(i))) {
                current++;
                maxConsecutive = Math.max(maxConsecutive, current);
            } else {
                current = 0;
            }
        }
        return maxConsecutive;
    }

    private int calcSimpleEngagement(Integer userId, int days) {
        int studyMin = 0;
        Integer m = studyDurationRecordMapper.getTotalStudyDuration(userId, days);
        if (m != null) studyMin = m;
        int testCount = 0;
        List<SectionTestSummary> tests = summaryMapper.selectCompletedByUser(userId);
        if (tests != null) testCount = tests.size();
        int qaCount = qaHistoryMapper.countByStudentIdAndDays(userId.longValue(), days);

        double dailyAvg = (double) studyMin / Math.max(days, 1);
        int score = (int) Math.min(100, dailyAvg * 1.2 + testCount * 2 + qaCount * 0.5);
        return Math.min(100, Math.max(0, score));
    }

    private List<CourseProgress> buildCourseProgress(Integer userId) {
        List<Course> courses = courseMapper.listByCreator(userId);
        if (courses == null || courses.isEmpty()) return new ArrayList<>();

        List<Integer> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        // 批量查询章节
        List<Chapter> allChapters = chapterMapper.listByCourseIds(courseIds);
        Map<Integer, List<Chapter>> chaptersByCourse = allChapters != null
                ? allChapters.stream().collect(Collectors.groupingBy(Chapter::getCourseId))
                : Collections.emptyMap();

        // 批量查询小节
        List<Integer> chapterIds = allChapters != null
                ? allChapters.stream().map(Chapter::getId).collect(Collectors.toList())
                : Collections.emptyList();
        List<Section> allSections = chapterIds.isEmpty()
                ? Collections.emptyList()
                : sectionMapper.listByChapterIds(chapterIds);
        Map<Integer, List<Section>> sectionsByChapter = allSections.stream()
                .collect(Collectors.groupingBy(Section::getChapterId));

        List<CourseProgress> result = new ArrayList<>();
        for (Course course : courses) {
            List<Chapter> chapters = chaptersByCourse.getOrDefault(course.getId(), Collections.emptyList());
            int totalSections = 0;
            int completedSections = 0;
            for (Chapter ch : chapters) {
                List<Section> sections = sectionsByChapter.getOrDefault(ch.getId(), Collections.emptyList());
                totalSections += sections.size();
                completedSections += (int) sections.stream()
                        .filter(s -> s.getIsCompleted() != null && s.getIsCompleted())
                        .count();
            }

            CourseProgress cp = new CourseProgress();
            cp.setCourseId(course.getId());
            cp.setCourseName(course.getCourseName());
            cp.setTotalSections(totalSections);
            cp.setCompletedSections(completedSections);
            cp.setCompletionRate(totalSections > 0
                    ? Math.round((double) completedSections / totalSections * 1000.0) / 10.0
                    : 0.0);
            cp.setStatus(completedSections == 0 ? "NOT_STARTED"
                    : completedSections >= totalSections ? "COMPLETED" : "IN_PROGRESS");
            result.add(cp);
        }
        return result;
    }

    private List<CourseStudyTime> buildCourseStudyTime(Integer userId, Integer courseId, int days) {
        List<Map<String, Object>> raw = studyDurationRecordMapper.getStudyDurationGroupByCourse(userId, days);
        if (raw == null || raw.isEmpty()) return new ArrayList<>();

        return raw.stream().map(m -> {
            CourseStudyTime cst = new CourseStudyTime();
            Object cid = m.get("courseId");
            cst.setCourseId(cid instanceof Number ? ((Number) cid).intValue() : 0);
            cst.setCourseName(String.valueOf(m.getOrDefault("courseName", "")));
            Object tm = m.get("totalMinutes");
            cst.setTotalMinutes(tm instanceof Number ? ((Number) tm).intValue() : 0);
            return cst;
        }).collect(Collectors.toList());
    }

}
