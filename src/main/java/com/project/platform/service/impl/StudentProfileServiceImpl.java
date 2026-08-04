package com.project.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.entity.StudentProfile;
import com.project.platform.mapper.StudentProfileMapper;
import com.project.platform.service.StudentProfileService;
import com.project.platform.vo.StudentProfileDashboardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生画像服务实现
 */
@Slf4j
@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentProfileMapper studentProfileMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentProfileServiceImpl(StudentProfileMapper studentProfileMapper) {
        this.studentProfileMapper = studentProfileMapper;
    }

    @Override
    public StudentProfile getProfileByStudentId(Integer studentId) {
        log.info("=== 查询学生画像，studentId: {}", studentId);
        return studentProfileMapper.selectByStudentId(studentId);
    }

    @Override
    public StudentProfileDashboardVO getDashboard(Integer studentId) {
        StudentProfile profile = studentProfileMapper.selectByStudentId(studentId);
        StudentProfileDashboardVO vo = new StudentProfileDashboardVO();

        if (profile == null) {
            log.warn("=== 学生画像不存在，返回默认空数据，studentId: {}", studentId);
            return buildEmptyDashboard();
        }

        vo.setStatsOverview(buildStatsOverview(profile));
        vo.setModuleOne(buildModuleOne(profile));
        vo.setCenterIndicators(buildCenterIndicators(profile));
        vo.setModuleSix(buildModuleSix(profile));
        vo.setModuleTwo(buildModuleTwo(profile));
        vo.setModuleThree(buildModuleThree(profile));
        vo.setModuleFour(buildModuleFour(profile));
        vo.setModuleFive(buildModuleFive(profile));
        vo.setLearningAnalysis(buildLearningAnalysis(profile));
        vo.setBottomModules(buildBottomModules(profile));

        return vo;
    }

    // ==================== 各模块组装 ====================

    private StudentProfileDashboardVO.StatsOverview buildStatsOverview(StudentProfile p) {
        StudentProfileDashboardVO.StatsOverview s = new StudentProfileDashboardVO.StatsOverview();
        s.setOverallScore(calcCompositeScore(p));
        s.setStudyMinutes(p.getAvgDailyStudyMinutes() != null ? p.getAvgDailyStudyMinutes().intValue() : 0);
        s.setConcentration(p.getAvgConcentration() != null ? p.getAvgConcentration().intValue() : 0);
        int knowSum = 0, knowCount = 0;
        Integer[] kd = {p.getConceptClarity(), p.getMemoryStability(), p.getTransferAbility(),
                p.getStructuralIntegrity(), p.getKnowledgeDepth()};
        for (Integer k : kd) { if (k != null) { knowSum += k; knowCount++; } }
        s.setKnowledgeMastery(knowCount > 0 ? knowSum / knowCount : 0);
        s.setCognitiveStyle("视觉型");
        boolean hasRisk = false;
        for (Integer k : kd) { if (k != null && k < 30) { hasRisk = true; break; } }
        s.setRiskWarning(hasRisk ? "需关注" : "正常");
        return s;
    }

    private static final String[] ERROR_TYPE_DEFAULTS = {"概念混淆", "计算失误", "逻辑错误", "记忆偏差", "应用错误"};
    private static final String[] SUBJECT_DEFAULTS = {"程序设计", "数据结构", "算法分析", "系统架构", "人工智能"};

    private StudentProfileDashboardVO.ModuleOne buildModuleOne(StudentProfile p) {
        StudentProfileDashboardVO.ModuleOne m = new StudentProfileDashboardVO.ModuleOne();

        // timeSeries: 从 weeklyStudyData JSON 解析
        m.setTimeSeries(parseTimeSeries(p.getWeeklyStudyData()));

        // errorPreference: 写死具体易错类型数据
        StudentProfileDashboardVO.BarPieGroup errorG = new StudentProfileDashboardVO.BarPieGroup();
        errorG.setBar(barData("易错偏好排行", "192,35,42",
                Arrays.asList("概念混淆", "计算失误", "逻辑错误", "记忆偏差", "应用错误"),
                Arrays.asList(60, 40, 80, 35, 50)));
        List<StudentProfileDashboardVO.PieItem> errorPieItems = new ArrayList<>();
        errorPieItems.add(pieItem("概念混淆", 60, "#a262f2"));
        errorPieItems.add(pieItem("计算失误", 40, "#2ca8fe"));
        errorPieItems.add(pieItem("逻辑错误", 80, "#feac2c"));
        errorPieItems.add(pieItem("记忆偏差", 35, "#c0232a"));
        errorPieItems.add(pieItem("应用错误", 50, "#2cd9fe"));
        errorPieItems.add(pieItem("其他", 30, "#252448"));
        errorG.setPie(pieData("易错类型分布", "#BE232A", errorPieItems));
        m.setErrorPreference(errorG);

        // subjectInterest: 写死具体学科数据
        StudentProfileDashboardVO.BarPieGroup subjectG = new StudentProfileDashboardVO.BarPieGroup();
        subjectG.setBar(barData("学科兴趣排行", "40,112,232",
                Arrays.asList("程序设计", "数据结构", "算法分析", "系统架构", "人工智能"),
                Arrays.asList(85, 72, 68, 55, 90)));
        List<StudentProfileDashboardVO.PieItem> subjectPieItems = new ArrayList<>();
        subjectPieItems.add(pieItem("程序设计", 85, "#feed2c"));
        subjectPieItems.add(pieItem("数据结构", 72, "#2ca8fe"));
        subjectPieItems.add(pieItem("算法分析", 68, "#feac2c"));
        subjectPieItems.add(pieItem("系统架构", 55, "#2c7bfe"));
        subjectPieItems.add(pieItem("人工智能", 90, "#a262f2"));
        subjectG.setPie(pieData("学科兴趣占比", "#2C7BFE", subjectPieItems));
        m.setSubjectInterest(subjectG);

        return m;
    }

    private StudentProfileDashboardVO.CenterIndicators buildCenterIndicators(StudentProfile p) {
        StudentProfileDashboardVO.CenterIndicators c = new StudentProfileDashboardVO.CenterIndicators();
        List<StudentProfileDashboardVO.IndicatorItem> items = new ArrayList<>();

        String[] titles = {"认知控制", "知识表征", "信息加工", "实践导向", "学习主动", "任务管理", "综合评分"};
        Integer[] scores = {
            p.getCognitiveControl() != null ? p.getCognitiveControl() : 50,
            p.getKnowledgeRepresentation() != null ? p.getKnowledgeRepresentation() : 50,
            p.getInformationProcessing() != null ? p.getInformationProcessing() : 50,
            p.getPracticeOrientation() != null ? p.getPracticeOrientation() : 50,
            p.getLearningInitiative() != null ? p.getLearningInitiative() : 50,
            p.getTaskManagement() != null ? p.getTaskManagement() : 50,
            calcCompositeScore(p)
        };

        for (int i = 0; i < titles.length; i++) {
            StudentProfileDashboardVO.IndicatorItem item = new StudentProfileDashboardVO.IndicatorItem();
            item.setTitle(titles[i]);
            item.setRate(scoreToRate(scores[i]));
            item.setStatus(scores[i] < 30);
            item.setTag(getIndicatorTag(i, scores[i]));
            item.setDescription(getIndicatorDesc(i, scores[i]));
            items.add(item);
        }
        c.setItems(items);
        return c;
    }

    private StudentProfileDashboardVO.ModuleSix buildModuleSix(StudentProfile p) {
        StudentProfileDashboardVO.ModuleSix m = new StudentProfileDashboardVO.ModuleSix();
        m.setRing1(buildErrorPie(p));
        m.setRing2(buildSubjectPie(p));
        m.setRing3(buildKnowledgePrefPie(p));
        return m;
    }

    private StudentProfileDashboardVO.ModuleTwo buildModuleTwo(StudentProfile p) {
        StudentProfileDashboardVO.ModuleTwo m = new StudentProfileDashboardVO.ModuleTwo();

        // 5 个知识基础维度
        List<StudentProfileDashboardVO.KnowledgeDimension> dims = new ArrayList<>();
        dims.add(kd("概念清晰度", p.getConceptClarity()));
        dims.add(kd("记忆稳固性", p.getMemoryStability()));
        dims.add(kd("迁移能力", p.getTransferAbility()));
        dims.add(kd("结构完整性", p.getStructuralIntegrity()));
        dims.add(kd("知识深度", p.getKnowledgeDepth()));
        m.setDimensions(dims);

        m.setPieA(buildDefaultPie("知识掌握", "#c0232a",
                new String[]{"优秀", "良好"}, Arrays.asList(nvl(p.getConceptClarity()), nvl(p.getMemoryStability())),
                new String[]{"#c0232a", "#2870e8"}));

        m.setPieB(buildDefaultPie("能力分布", "#c0232a",
                new String[]{"迁移", "结构", "深度"},
                Arrays.asList(nvl(p.getTransferAbility()), nvl(p.getStructuralIntegrity()), nvl(p.getKnowledgeDepth())),
                new String[]{"#c2232a", "#fe672c", "#a262f2", "#2870e8", "#feed2c"}));

        return m;
    }

    private StudentProfileDashboardVO.ModuleThree buildModuleThree(StudentProfile p) {
        StudentProfileDashboardVO.ModuleThree m = new StudentProfileDashboardVO.ModuleThree();

        StudentProfileDashboardVO.AreaConfig area = new StudentProfileDashboardVO.AreaConfig();
        area.setTitle("【学习投入趋势】");
        area.setColor("#75deef");
        area.setName(Arrays.asList("（分钟）", "（%）"));

        List<StudentProfileDashboardVO.AreaSeries> series = new ArrayList<>();
        series.add(areaSeries("视频学习", new String[]{"#feed2c", "#353103"}, Arrays.asList(240, 210, 185, 260, 195, 220, 250)));
        series.add(areaSeries("练习时长", new String[]{"#2871ea", "#0a1b41"}, Arrays.asList(120, 150, 100, 180, 140, 130, 160)));
        series.add(areaSeries("专注度", new String[]{"#935adf", "#230f3e"}, Arrays.asList(75, 80, 70, 85, 78, 72, 82)));
        series.add(areaSeries("正确率", new String[]{"#e65f2d", "#551f0b"}, Arrays.asList(88, 92, 85, 90, 87, 91, 89)));
        area.setData(series);
        m.setAreaConfig(area);

        // 下方多彩条形图
        List<StudentProfileDashboardVO.ColorBarItem> items = new ArrayList<>();
        String[] names = {"概念清晰度", "记忆稳固性", "迁移能力", "结构完整性", "知识深度", "信息加工", "知识表征", "认知控制"};
        String[] colors = {"#2c7bfe", "#c2232a", "#feed2c", "#a262f2", "#62d5f2", "#fe672c", "#69f262", "#2ca8fe"};
        Integer[] vals = {
            nvl(p.getConceptClarity()), nvl(p.getMemoryStability()), nvl(p.getTransferAbility()),
            nvl(p.getStructuralIntegrity()), nvl(p.getKnowledgeDepth()),
            nvl(p.getInformationProcessing()), nvl(p.getKnowledgeRepresentation()), nvl(p.getCognitiveControl())
        };
        for (int i = 0; i < names.length; i++) {
            items.add(colorBarItem(names[i], vals[i], colors[i]));
        }
        m.setBehaviorItems(items);
        return m;
    }

    private StudentProfileDashboardVO.ModuleFour buildModuleFour(StudentProfile p) {
        StudentProfileDashboardVO.ModuleFour m = new StudentProfileDashboardVO.ModuleFour();
        List<StudentProfileDashboardVO.FunnelItem> items = new ArrayList<>();
        items.add(funnelItem("偏文字", nvl(p.getTextPreference())));
        items.add(funnelItem("偏图像", nvl(p.getImagePreference())));
        items.add(funnelItem("偏示例", nvl(p.getExamplePreference())));
        items.add(funnelItem("偏视频", nvl(p.getVideoPreference())));
        items.add(funnelItem("综合偏好", calcAvg(p.getTextPreference(), p.getImagePreference(),
                p.getExamplePreference(), p.getVideoPreference())));
        m.setItems(items);
        return m;
    }

    private StudentProfileDashboardVO.ModuleFive buildModuleFive(StudentProfile p) {
        StudentProfileDashboardVO.ModuleFive m = new StudentProfileDashboardVO.ModuleFive();
        List<StudentProfileDashboardVO.UsageItem> items = new ArrayList<>();
        // name, value, 8维使用时长数据
        String[][] toolData = {
            {"大模型对话", "380", "3600,7200,4800,9000,5200,1800,2400,600"},
            {"PPT生成", "300", "2800,5800,3600,7000,6400,3600,5400,4600"},
            {"思维导图", "240", "2600,3200,5800,2000,3200,5800,7400,2600"},
            {"资源推荐", "200", "1600,4200,2800,1000,7200,5800,3400,5600"},
            {"沉浸式课程", "150", "5600,6200,6800,3000,1200,5800,4400,2500"},
            {"虚拟教师", "120", "6600,8200,5800,4000,2200,3800,7400,3600"},
            {"练习测试", "100", "7600,1200,3800,7000,1200,3800,5400,2600"},
            {"知识检索", "80", "6600,1200,2800,3000,6200,5800,4400,1600"}
        };
        for (String[] td : toolData) {
            StudentProfileDashboardVO.UsageItem item = new StudentProfileDashboardVO.UsageItem();
            item.setName(td[0]);
            item.setValue(Integer.parseInt(td[1]));
            item.setData(Arrays.stream(td[2].split(",")).map(Integer::parseInt).collect(Collectors.toList()));
            items.add(item);
        }
        m.setItems(items);
        return m;
    }

    private StudentProfileDashboardVO.LearningAnalysis buildLearningAnalysis(StudentProfile p) {
        StudentProfileDashboardVO.LearningAnalysis la = new StudentProfileDashboardVO.LearningAnalysis();

        // 模块一：学习投入度雷达 + 面积图
        la.setEngagementRadar(buildEngagementRadar(p));
        la.setEngagementArea(buildEngagementArea());

        // 模块二：认知风格雷达 + 趋势
        la.setCognitiveRadar(buildCognitiveRadar(p));
        la.setCognitiveTrend(parseTimeSeries(p.getWeeklyStudyData()));

        // 模块三：社交维度雷达
        la.setSocialRadar(buildDefaultRadar("社交维度", new String[]{"互动频率", "协作能力", "分享意愿", "沟通表达", "团队贡献", "领导力"},
                new double[]{nvl(p.getLearningInitiative()), 65, nvl(p.getKnowledgeRepresentation()),
                        70, nvl(p.getPracticeOrientation()), 55}, "#FA8486"));

        // 模块四：内容维度雷达
        la.setContentRadar(buildDefaultRadar("内容维度", new String[]{"视频学习", "文章阅读", "练习测试", "项目实战", "讨论交流", "资源收藏"},
                new double[]{nvl(p.getVideoPreference()), nvl(p.getTextPreference()),
                        nvl(p.getPracticeOrientation()), nvl(p.getTransferAbility()),
                        nvl(p.getLearningInitiative()), 60}, "#D91748"));

        // 综合分数
        la.setOverallScore(calcCompositeScore(p));

        return la;
    }

    private StudentProfileDashboardVO.BottomModules buildBottomModules(StudentProfile p) {
        StudentProfileDashboardVO.BottomModules bm = new StudentProfileDashboardVO.BottomModules();
        bm.setModule5Trend(parseTimeSeries(p.getWeeklyStudyData()));
        bm.setModule6Trend(parseTimeSeries(p.getWeeklyStudyData()));
        bm.setModule7Trend(parseTimeSeries(p.getWeeklyStudyData()));
        bm.setModule8Trend(parseTimeSeries(p.getWeeklyStudyData()));
        return bm;
    }

    // ==================== 辅助构建方法 ====================

    private List<StudentProfileDashboardVO.TimeSeriesPoint> parseTimeSeries(String weeklyStudyData) {
        List<StudentProfileDashboardVO.TimeSeriesPoint> points = new ArrayList<>();
        if (weeklyStudyData == null || weeklyStudyData.isEmpty()) {
            return generateDefaultTimeSeries();
        }
        try {
            // 先尝试 Object 格式（如 {"monday":{...}, "tuesday":{...}}）
            try {
                Map<String, Map<String, Object>> objFormat = objectMapper.readValue(weeklyStudyData,
                        new TypeReference<Map<String, Map<String, Object>>>() {});
                String[] dayOrder = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
                String[] dayLabels = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                for (int i = 0; i < dayOrder.length; i++) {
                    Map<String, Object> dayData = objFormat.get(dayOrder[i]);
                    if (dayData == null) continue;
                    StudentProfileDashboardVO.TimeSeriesPoint pt = new StudentProfileDashboardVO.TimeSeriesPoint();
                    pt.setDate(dayLabels[i]);
                    int videoMin = toInt(dayData.get("video_minutes"));
                    int practiceMin = toInt(dayData.get("practice_minutes"));
                    int concentration = toInt(dayData.get("concentration"));
                    pt.setVisits(videoMin + practiceMin);
                    pt.setVisitors(concentration);
                    points.add(pt);
                }
                if (!points.isEmpty()) return points;
            } catch (Exception ignored) {
                // 不是 Object 格式，尝试数组格式
            }

            // 再尝试数组格式 [{"date":"...", "visits":N, "visitors":N}, ...]
            List<Map<String, Object>> raw = objectMapper.readValue(weeklyStudyData,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> entry : raw) {
                StudentProfileDashboardVO.TimeSeriesPoint pt = new StudentProfileDashboardVO.TimeSeriesPoint();
                pt.setDate(Objects.toString(entry.getOrDefault("date", ""), ""));
                pt.setVisits(toInt(entry.get("visits")));
                pt.setVisitors(toInt(entry.get("visitors")));
                points.add(pt);
            }
        } catch (Exception e) {
            log.warn("解析 weeklyStudyData JSON 失败，使用默认时序数据", e);
            return generateDefaultTimeSeries();
        }
        return points.isEmpty() ? generateDefaultTimeSeries() : points;
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private List<StudentProfileDashboardVO.TimeSeriesPoint> generateDefaultTimeSeries() {
        List<StudentProfileDashboardVO.TimeSeriesPoint> points = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            StudentProfileDashboardVO.TimeSeriesPoint pt = new StudentProfileDashboardVO.TimeSeriesPoint();
            java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
            pt.setDate(d.getYear() + "." + d.getMonthValue() + "." + d.getDayOfMonth());
            pt.setVisits((int) (Math.random() * 6));
            pt.setVisitors((int) (Math.random() * 600));
            points.add(pt);
        }
        return points;
    }

    private StudentProfileDashboardVO.BarPieGroup parseBarPieGroup(String json, String defaultName,
            String barColor, String[] pieColors, String[] defaultCategoryNames) {
        StudentProfileDashboardVO.BarPieGroup g = new StudentProfileDashboardVO.BarPieGroup();
        if (json == null || json.isEmpty()) {
            g.setBar(buildDefaultBar(defaultName, barColor, defaultCategoryNames));
            g.setPie(buildDefaultPie(defaultName + "占比", pieColors[0], defaultCategoryNames,
                    randomInts(defaultCategoryNames.length, 10, 100), pieColors));
            return g;
        }
        List<Map<String, Object>> items = parseJsonToList(json);
        if (items == null) {
            g.setBar(buildDefaultBar(defaultName, barColor, defaultCategoryNames));
            g.setPie(buildDefaultPie(defaultName + "占比", pieColors[0], defaultCategoryNames,
                    randomInts(defaultCategoryNames.length, 10, 100), pieColors));
            return g;
        }
        List<String> names = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        List<StudentProfileDashboardVO.PieItem> pieItems = new ArrayList<>();
        for (int i = 0; i < items.size() && i < pieColors.length; i++) {
            Map<String, Object> item = items.get(i);
            String name = Objects.toString(item.getOrDefault("name", "分类" + (i + 1)), "分类" + (i + 1));
            int value = item.get("value") instanceof Number ? ((Number) item.get("value")).intValue()
                    : (int) (Math.random() * 90 + 10);
            names.add(name);
            values.add(value);
            pieItems.add(pieItem(name, value, pieColors[i]));
        }
        g.setBar(barData(defaultName, barColor, names, values));
        g.setPie(pieData(defaultName + "占比", pieColors[0], pieItems));
        return g;
    }

    /**
     * 解析 JSON 字符串为 List<Map>，兼容 Object 格式和 Array 格式
     */
    private List<Map<String, Object>> parseJsonToList(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            // 先试数组格式
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e1) {
            try {
                // 再试 Object 格式，将每个 entry 转为一个 Map（注入 key 为 name）
                Map<String, Map<String, Object>> objFormat = objectMapper.readValue(json,
                        new TypeReference<Map<String, Map<String, Object>>>() {});
                List<Map<String, Object>> list = new ArrayList<>();
                for (Map.Entry<String, Map<String, Object>> entry : objFormat.entrySet()) {
                    Map<String, Object> item = new HashMap<>(entry.getValue());
                    item.putIfAbsent("name", entry.getKey());
                    list.add(item);
                }
                return list.isEmpty() ? null : list;
            } catch (Exception e2) {
                log.warn("无法解析 JSON（既非数组也非对象格式）", e2);
                return null;
            }
        }
    }

    private StudentProfileDashboardVO.PieData buildErrorPie(StudentProfile p) {
        List<StudentProfileDashboardVO.PieItem> items = new ArrayList<>();
        items.add(pieItem("概念混淆", 60, "#a262f2"));
        items.add(pieItem("计算失误", 40, "#2ca8fe"));
        items.add(pieItem("逻辑错误", 80, "#feac2c"));
        items.add(pieItem("记忆偏差", 35, "#c0232a"));
        items.add(pieItem("应用错误", 50, "#2cd9fe"));
        items.add(pieItem("其他", 30, "#ff787e"));
        return pieData("易错类型分布", "#BE232A", items);
    }

    private StudentProfileDashboardVO.PieData buildSubjectPie(StudentProfile p) {
        List<StudentProfileDashboardVO.PieItem> items = new ArrayList<>();
        items.add(pieItem("程序设计", 85, "#feed2c"));
        items.add(pieItem("数据结构", 72, "#2ca8fe"));
        items.add(pieItem("算法分析", 68, "#feac2c"));
        items.add(pieItem("系统架构", 55, "#2c7bfe"));
        items.add(pieItem("人工智能", 90, "#a262f2"));
        return pieData("学科兴趣分布", "#2C7BFE", items);
    }

    private StudentProfileDashboardVO.PieData buildKnowledgePrefPie(StudentProfile p) {
        List<StudentProfileDashboardVO.PieItem> items = new ArrayList<>();
        items.add(pieItem("偏文字", nvl(p.getTextPreference()), "#69f262"));
        items.add(pieItem("偏图像", nvl(p.getImagePreference()), "#c0232a"));
        items.add(pieItem("偏示例", nvl(p.getExamplePreference()), "#2cfcfe"));
        items.add(pieItem("偏视频", nvl(p.getVideoPreference()), "#252448"));
        items.add(pieItem("其他", 30, "#a262f2"));
        return pieData("知识获取偏好", "#69f262", items);
    }

    private StudentProfileDashboardVO.PieData buildPiFromJsonOrDefault(String json, String title,
            String color, String[] pieColors, String[] defaultCategoryNames) {
        if (json == null || json.isEmpty()) {
            return buildDefaultPie(title, color, defaultCategoryNames,
                    randomInts(defaultCategoryNames.length, 10, 100), pieColors);
        }
        List<Map<String, Object>> raw = parseJsonToList(json);
        if (raw == null) {
            return buildDefaultPie(title, color, defaultCategoryNames,
                    randomInts(defaultCategoryNames.length, 10, 100), pieColors);
        }
        List<StudentProfileDashboardVO.PieItem> items = new ArrayList<>();
        for (int i = 0; i < raw.size() && i < pieColors.length; i++) {
            Map<String, Object> item = raw.get(i);
            items.add(pieItem(
                    Objects.toString(item.getOrDefault("name", "分类" + (i + 1)), "分类" + (i + 1)),
                    item.get("value") instanceof Number ? ((Number) item.get("value")).intValue() : (int) (Math.random() * 90 + 10),
                    pieColors[i]));
        }
        if (items.size() < raw.size() + 1 && raw.size() < pieColors.length) {
            items.add(pieItem("其他", 30, pieColors[Math.min(raw.size(), pieColors.length - 1)]));
        }
        return pieData(title, color, items);
    }

    private StudentProfileDashboardVO.RadarData buildEngagementRadar(StudentProfile p) {
        return buildDefaultRadar("学习投入度", new String[]{"日均时长", "专注度", "连续性", "完成率", "互动次数", "资源使用"},
                new double[]{normalizeTo100(nvl(p.getAvgDailyStudyMinutes()), 480),
                        nvl(p.getAvgConcentration()),
                        nvl(p.getLearningInitiative()),
                        nvl(p.getPracticeOrientation()),
                        nvl(p.getCognitiveControl()),
                        nvl(p.getInformationProcessing())},
                "#0DF5F8");
    }

    private StudentProfileDashboardVO.RadarData buildCognitiveRadar(StudentProfile p) {
        return buildDefaultRadar("认知风格", new String[]{"信息加工", "知识表征", "学习主动性", "认知控制", "实践导向", "任务管理"},
                new double[]{nvl(p.getInformationProcessing()), nvl(p.getKnowledgeRepresentation()),
                        nvl(p.getLearningInitiative()), nvl(p.getCognitiveControl()),
                        nvl(p.getPracticeOrientation()), nvl(p.getTaskManagement())},
                "#55D35B");
    }

    private StudentProfileDashboardVO.AreaConfig buildEngagementArea() {
        StudentProfileDashboardVO.AreaConfig area = new StudentProfileDashboardVO.AreaConfig();
        area.setColor("#5CB1C1");
        area.setName(Arrays.asList("（次）", "（人）"));
        List<StudentProfileDashboardVO.AreaSeries> series = new ArrayList<>();
        series.add(areaSeries("次数", new String[]{"#9e70ff", "#6e5eff"}, randomInts(7, 30, 260)));
        series.add(areaSeries("人数", new String[]{"#48cefd", "#5356f1"}, randomInts(7, 10, 240)));
        area.setData(series);
        return area;
    }

    private StudentProfileDashboardVO.RadarData buildDefaultRadar(String title, String[] indicators,
            double[] values, String color) {
        StudentProfileDashboardVO.RadarData r = new StudentProfileDashboardVO.RadarData();
        r.setTitle(title);
        r.setPosition(Arrays.asList("center", "85%"));
        r.setCenter(Arrays.asList("50%", "50%"));
        List<StudentProfileDashboardVO.RadarIndicator> inds = new ArrayList<>();
        for (String ind : indicators) {
            StudentProfileDashboardVO.RadarIndicator ri = new StudentProfileDashboardVO.RadarIndicator();
            ri.setText(ind);
            inds.add(ri);
        }
        r.setIndicator(inds);
        StudentProfileDashboardVO.RadarSeries rs = new StudentProfileDashboardVO.RadarSeries();
        rs.setName("得分");
        rs.setColor(color);
        rs.setValue(Arrays.stream(values).boxed().collect(Collectors.toList()));
        r.setData(Collections.singletonList(rs));
        return r;
    }

    // ==================== 工厂方法 ====================

    private StudentProfileDashboardVO.BarData barData(String name, String color, List<String> labels, List<Integer> values) {
        StudentProfileDashboardVO.BarData b = new StudentProfileDashboardVO.BarData();
        b.setName(name);
        b.setNumber(values.stream().mapToInt(Integer::intValue).sum() + "次");
        b.setData(labels);
        b.setColor(color);
        b.setValue(values);
        return b;
    }

    private StudentProfileDashboardVO.BarData buildDefaultBar(String name, String color, String[] labels) {
        return barData(name, color,
                Arrays.asList(labels),
                randomInts(labels.length, 10, 100));
    }

    private StudentProfileDashboardVO.PieData pieData(String title, String color,
            List<StudentProfileDashboardVO.PieItem> items) {
        StudentProfileDashboardVO.PieData p = new StudentProfileDashboardVO.PieData();
        p.setTitle(title);
        p.setColor(color);
        p.setData(items);
        return p;
    }

    private StudentProfileDashboardVO.PieData buildDefaultPie(String title, String color,
            String[] names, List<Integer> values, String[] colors) {
        List<StudentProfileDashboardVO.PieItem> items = new ArrayList<>();
        for (int i = 0; i < names.length && i < values.size(); i++) {
            items.add(pieItem(names[i], values.get(i), colors[i % colors.length]));
        }
        return pieData(title, color, items);
    }

    private StudentProfileDashboardVO.PieItem pieItem(String name, int value, String color) {
        StudentProfileDashboardVO.PieItem item = new StudentProfileDashboardVO.PieItem();
        item.setName(name);
        item.setValue(value);
        StudentProfileDashboardVO.PieItem.ItemStyle style = new StudentProfileDashboardVO.PieItem.ItemStyle();
        style.setColor(color);
        item.setItemStyle(style);
        return item;
    }

    private StudentProfileDashboardVO.AreaSeries areaSeries(String name, String[] colors, List<Integer> data) {
        StudentProfileDashboardVO.AreaSeries s = new StudentProfileDashboardVO.AreaSeries();
        s.setName(name);
        s.setColor(Arrays.asList(colors));
        s.setData(data);
        return s;
    }

    private StudentProfileDashboardVO.ColorBarItem colorBarItem(String name, int value, String color) {
        StudentProfileDashboardVO.ColorBarItem item = new StudentProfileDashboardVO.ColorBarItem();
        item.setName(name);
        item.setValue(value);
        StudentProfileDashboardVO.ColorBarItem.ItemStyle style = new StudentProfileDashboardVO.ColorBarItem.ItemStyle();
        style.setColor(color);
        item.setItemStyle(style);
        return item;
    }

    private StudentProfileDashboardVO.FunnelItem funnelItem(String name, int value) {
        StudentProfileDashboardVO.FunnelItem item = new StudentProfileDashboardVO.FunnelItem();
        item.setName(name);
        item.setValue(value);
        return item;
    }

    private StudentProfileDashboardVO.KnowledgeDimension kd(String name, Integer score) {
        StudentProfileDashboardVO.KnowledgeDimension k = new StudentProfileDashboardVO.KnowledgeDimension();
        k.setName(name);
        k.setScore(nvl(score));
        return k;
    }

    // ==================== 工具方法 ====================

    private int nvl(Integer val) {
        return val != null ? val : 50;
    }

    private int nvl(Double val) {
        return val != null ? val.intValue() : 50;
    }

    private String getIndicatorTag(int index, int score) {
        boolean high = score > 50;
        switch (index) {
            case 0: return high ? "反思型" : "冲动型";
            case 1: return high ? "视觉型" : "言语型";
            case 2: return high ? "整体型" : "序列型";
            case 3: return high ? "实践型" : "理论型";
            case 4: return high ? "主动型" : "被动型";
            case 5: return high ? "规划型" : "随机型";
            default: return "综合画像";
        }
    }

    private String getIndicatorDesc(int index, int score) {
        boolean high = score > 50;
        switch (index) {
            case 0:
                return high
                    ? "面对问题时倾向于深思熟虑后再作答，答题速度较慢但准确率高。建议在限时测试中适当提升决策速度。"
                    : "答题迅速、反应敏捷，但容易因粗心导致失误。建议在关键题目上放慢节奏，增加二次检查习惯。";
            case 1:
                return high
                    ? "对图表、图像类内容理解深刻，善于从可视化材料中提取信息。推荐多使用思维导图、流程图辅助学习。"
                    : "擅长从文字和语言描述中获取知识，阅读能力强。可适当增加图表化笔记来强化记忆效果。";
            case 2:
                return high
                    ? "习惯先建立整体知识框架再深入细节，擅长把握知识间的关联。建议在框架基础上增加专项练习巩固细节。"
                    : "偏好按顺序逐步学习，对单个知识点的掌握扎实。建议定期进行知识整合，构建跨章节的知识联系。";
            case 3:
                return high
                    ? "倾向于通过动手实践和项目实战来掌握知识，工程应用能力强。建议在实践后补充理论原理加深理解。"
                    : "偏好先理解理论原理再动手操作，基础扎实但应用转换较慢。建议增加小型项目练习提升实践能力。";
            case 4:
                return high
                    ? "学习主动性较强，能自主规划学习路径并积极探索新知识。保持此习惯，可尝试挑战更高难度内容。"
                    : "学习较为被动，倾向于跟随课程安排和外部驱动。建议设定短期学习目标和自我激励机制，逐步培养主动性。";
            case 5:
                return high
                    ? "善于制定学习计划并按部就班执行，时间管理能力强。建议在计划中预留弹性空间应对突发情况。"
                    : "学习方式灵活随性，能根据兴趣快速切换学习内容。建议使用周计划模板增加学习结构的稳定性。";
            default:
                return "综合各维度评分，该生整体学习画像为" + (score >= 60 ? "良好" : score >= 40 ? "中等" : "待提升") + "水平。";
        }
    }

    private int scoreToRate(int score) {
        if (score >= 80) return 5;
        if (score >= 60) return 4;
        if (score >= 40) return 3;
        if (score >= 20) return 2;
        return 1;
    }

    private int calcCompositeScore(StudentProfile p) {
        int sum = 0, count = 0;
        Integer[] fields = {p.getConceptClarity(), p.getMemoryStability(), p.getTransferAbility(),
                p.getStructuralIntegrity(), p.getKnowledgeDepth(), p.getInformationProcessing(),
                p.getKnowledgeRepresentation(), p.getLearningInitiative(), p.getCognitiveControl(),
                p.getPracticeOrientation(), p.getTaskManagement()};
        for (Integer f : fields) {
            if (f != null) { sum += f; count++; }
        }
        return count > 0 ? sum / count : 50;
    }

    private int calcAvg(Integer a, Integer b, Integer c, Integer d) {
        int sum = 0, count = 0;
        if (a != null) { sum += a; count++; }
        if (b != null) { sum += b; count++; }
        if (c != null) { sum += c; count++; }
        if (d != null) { sum += d; count++; }
        return count > 0 ? sum / count : 50;
    }

    private double normalizeTo100(double value, double max) {
        return max > 0 ? Math.min(100, (value / max) * 100) : value;
    }

    private List<Integer> randomInts(int count, int min, int max) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add((int) (Math.random() * (max - min) + min));
        }
        return list;
    }

    private StudentProfileDashboardVO buildEmptyDashboard() {
        return new StudentProfileDashboardVO();
    }

    @Override
    public StudentProfile saveOrUpdateProfile(StudentProfile profile) {
        log.info("=== 保存或更新学生画像，studentId: {}", profile.getStudentId());
        
        // 查询是否已存在
        StudentProfile existing = studentProfileMapper.selectByStudentId(profile.getStudentId());
        
        if (existing == null) {
            // 首次创建
            profile.setVersion(1);
            profile.setCreateTime(LocalDateTime.now());
            profile.setUpdateTime(LocalDateTime.now());
            studentProfileMapper.insert(profile);
            log.info("=== 首次创建学生画像成功，studentId: {}, profileId: {}", 
                    profile.getStudentId(), profile.getId());
        } else {
            // 增量更新：保留原有数据，只更新非空字段
            updateProfileIncrementally(existing, profile);
            existing.setVersion(existing.getVersion() + 1);
            existing.setUpdateTime(LocalDateTime.now());
            studentProfileMapper.updateById(existing);
            profile = existing;
            log.info("=== 增量更新学生画像成功，studentId: {}, version: {}", 
                    profile.getStudentId(), profile.getVersion());
        }
        
        return profile;
    }

    /**
     * 增量更新画像：只更新非空字段
     */
    private void updateProfileIncrementally(StudentProfile existing, StudentProfile newProfile) {
        // 基础信息维度
        if (newProfile.getUsername() != null && !newProfile.getUsername().isEmpty()) {
            existing.setUsername(newProfile.getUsername());
        }
        if (newProfile.getNickname() != null && !newProfile.getNickname().isEmpty()) {
            existing.setNickname(newProfile.getNickname());
        }
        if (newProfile.getEmail() != null && !newProfile.getEmail().isEmpty()) {
            existing.setEmail(newProfile.getEmail());
        }
        if (newProfile.getMajor() != null && !newProfile.getMajor().isEmpty()) {
            existing.setMajor(newProfile.getMajor());
        }
        if (newProfile.getGrade() != null && !newProfile.getGrade().isEmpty()) {
            existing.setGrade(newProfile.getGrade());
        }
        if (newProfile.getLearningGoal() != null && !newProfile.getLearningGoal().isEmpty()) {
            existing.setLearningGoal(newProfile.getLearningGoal());
        }
        if (newProfile.getLearningStyle() != null && !newProfile.getLearningStyle().isEmpty()) {
            existing.setLearningStyle(newProfile.getLearningStyle());
        }
        if (newProfile.getWeaknessPoints() != null && !newProfile.getWeaknessPoints().isEmpty()) {
            existing.setWeaknessPoints(newProfile.getWeaknessPoints());
        }
        if (newProfile.getPreferredStyle() != null && !newProfile.getPreferredStyle().isEmpty()) {
            existing.setPreferredStyle(newProfile.getPreferredStyle());
        }
        
        // 知识基础维度（0-100分）
        if (newProfile.getConceptClarity() != null) {
            existing.setConceptClarity(newProfile.getConceptClarity());
        }
        if (newProfile.getMemoryStability() != null) {
            existing.setMemoryStability(newProfile.getMemoryStability());
        }
        if (newProfile.getTransferAbility() != null) {
            existing.setTransferAbility(newProfile.getTransferAbility());
        }
        if (newProfile.getStructuralIntegrity() != null) {
            existing.setStructuralIntegrity(newProfile.getStructuralIntegrity());
        }
        if (newProfile.getKnowledgeDepth() != null) {
            existing.setKnowledgeDepth(newProfile.getKnowledgeDepth());
        }
        
        // 认知风格维度（0-100分）
        if (newProfile.getInformationProcessing() != null) {
            existing.setInformationProcessing(newProfile.getInformationProcessing());
        }
        if (newProfile.getKnowledgeRepresentation() != null) {
            existing.setKnowledgeRepresentation(newProfile.getKnowledgeRepresentation());
        }
        if (newProfile.getLearningInitiative() != null) {
            existing.setLearningInitiative(newProfile.getLearningInitiative());
        }
        if (newProfile.getCognitiveControl() != null) {
            existing.setCognitiveControl(newProfile.getCognitiveControl());
        }
        if (newProfile.getPracticeOrientation() != null) {
            existing.setPracticeOrientation(newProfile.getPracticeOrientation());
        }
        if (newProfile.getTaskManagement() != null) {
            existing.setTaskManagement(newProfile.getTaskManagement());
        }
        
        // 学习投入度维度
        if (newProfile.getAvgDailyStudyMinutes() != null) {
            existing.setAvgDailyStudyMinutes(newProfile.getAvgDailyStudyMinutes());
        }
        if (newProfile.getAvgConcentration() != null) {
            existing.setAvgConcentration(newProfile.getAvgConcentration());
        }
        
        // 易错偏好维度
        if (newProfile.getErrorPreferences() != null && !newProfile.getErrorPreferences().isEmpty()) {
            existing.setErrorPreferences(newProfile.getErrorPreferences());
        }
        if (newProfile.getErrorTypeCount() != null) {
            existing.setErrorTypeCount(newProfile.getErrorTypeCount());
        }
        if (newProfile.getHighFrequencyErrorCount() != null) {
            existing.setHighFrequencyErrorCount(newProfile.getHighFrequencyErrorCount());
        }
        if (newProfile.getAvgErrorRate() != null) {
            existing.setAvgErrorRate(newProfile.getAvgErrorRate());
        }
        
        // 知识获取偏好维度（0-100%）
        if (newProfile.getTextPreference() != null) {
            existing.setTextPreference(newProfile.getTextPreference());
        }
        if (newProfile.getImagePreference() != null) {
            existing.setImagePreference(newProfile.getImagePreference());
        }
        if (newProfile.getExamplePreference() != null) {
            existing.setExamplePreference(newProfile.getExamplePreference());
        }
        if (newProfile.getVideoPreference() != null) {
            existing.setVideoPreference(newProfile.getVideoPreference());
        }
        
        // 学科兴趣领域维度
        if (newProfile.getSubjectInterests() != null && !newProfile.getSubjectInterests().isEmpty()) {
            existing.setSubjectInterests(newProfile.getSubjectInterests());
        }
        if (newProfile.getInterestSummary() != null && !newProfile.getInterestSummary().isEmpty()) {
            existing.setInterestSummary(newProfile.getInterestSummary());
        }
        if (newProfile.getRecommendedPath() != null && !newProfile.getRecommendedPath().isEmpty()) {
            existing.setRecommendedPath(newProfile.getRecommendedPath());
        }
        
        // 元数据
        if (newProfile.getCognitiveStyleSummary() != null && !newProfile.getCognitiveStyleSummary().isEmpty()) {
            existing.setCognitiveStyleSummary(newProfile.getCognitiveStyleSummary());
        }
        if (newProfile.getWeeklyStudyData() != null && !newProfile.getWeeklyStudyData().isEmpty()) {
            existing.setWeeklyStudyData(newProfile.getWeeklyStudyData());
        }
    }

    @Override
    public void deleteProfile(Integer studentId) {
        log.info("=== 删除学生画像，studentId: {}", studentId);
        studentProfileMapper.deleteByStudentId(studentId);
    }
}
