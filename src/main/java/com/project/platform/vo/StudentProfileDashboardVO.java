package com.project.platform.vo;

import lombok.Data;
import java.util.List;

/**
 * 学生画像数据大屏 - 综合数据 VO
 * 按大屏模块分组，数据格式直接匹配前端图表组件
 */
@Data
public class StudentProfileDashboardVO {

    /** Page1 综合指标概览面板 */
    private StatsOverview statsOverview;

    /** Page1 模块一（左上）：学习投入趋势 + 易错偏好 + 学科兴趣 */
    private ModuleOne moduleOne;

    /** Page1 中心人物：7 个心理/行为指标 */
    private CenterIndicators centerIndicators;

    /** Page1 模块六（右上）：3 个环形图 */
    private ModuleSix moduleSix;

    /** Page1 模块二（左下行）：知识基础维度 */
    private ModuleTwo moduleTwo;

    /** Page1 模块三：学习行为分析 */
    private ModuleThree moduleThree;

    /** Page1 模块四：知识获取漏斗 */
    private ModuleFour moduleFour;

    /** Page1 模块五：应用使用分析 */
    private ModuleFive moduleFive;

    /** Page2 学习分析：雷达图 + 中心分数 */
    private LearningAnalysis learningAnalysis;

    /** Page2 底部模块五~八 */
    private BottomModules bottomModules;

    // ==================== 嵌套结构 ====================

    /** 综合指标概览（6 个核心指标卡片） */
    @Data
    public static class StatsOverview {
        private Integer overallScore;     // 综合评分 0-100
        private Integer studyMinutes;     // 日均学习分钟
        private Integer concentration;    // 平均专注度 0-100
        private Integer knowledgeMastery; // 知识掌握率 0-100
        private String cognitiveStyle;    // 认知风格（短标签）
        private String riskWarning;       // 风险预警（短标签）
    }

    /** 模块一：包含 3 个子区域 */
    @Data
    public static class ModuleOne {
        /** left1: 学习投入时间序列（双区域图） */
        private List<TimeSeriesPoint> timeSeries;

        /** left2: 易错偏好排行 + 分布 */
        private BarPieGroup errorPreference;

        /** left3: 学科兴趣排行 + 分布 */
        private BarPieGroup subjectInterest;
    }

    /** 时间序列点 */
    @Data
    public static class TimeSeriesPoint {
        private String date;          // 日期 如 "2026.7.11"
        private Integer visits;       // 访问次数
        private Integer visitors;     // 访问人数
    }

    /** 柱状图 + 饼图组合 */
    @Data
    public static class BarPieGroup {
        private BarData bar;
        private PieData pie;
    }

    /** 柱状图数据 */
    @Data
    public static class BarData {
        private String name;          // 数据名称
        private String number;        // 总次数
        private List<String> data;    // 排行标签
        private String color;         // RGB 颜色 "R,G,B"
        private List<Integer> value;  // 排行数值
    }

    /** 饼图/环形图数据 */
    @Data
    public static class PieData {
        private String title;
        private String color;         // 主色调
        private List<PieItem> data;
    }

    /** 饼图数据项 */
    @Data
    public static class PieItem {
        private Integer value;
        private String name;
        private ItemStyle itemStyle;

        @Data
        public static class ItemStyle {
            private String color;
        }
    }

    /** 中心人物 7 个指标 */
    @Data
    public static class CenterIndicators {
        private List<IndicatorItem> items;
    }

    @Data
    public static class IndicatorItem {
        private String title;         // 指标名称
        private Integer rate;         // 评分 0-5
        private Boolean status;       // 是否异常
        private String tag;           // 短标签（如"视觉型"）
        private String description;   // 详细描述
    }

    /** 模块六：3 个环形图 */
    @Data
    public static class ModuleSix {
        private PieData ring1;        // 易错类型分布
        private PieData ring2;        // 学科兴趣分布
        private PieData ring3;        // 知识获取偏好分布
    }

    /** 模块二：知识基础维度 */
    @Data
    public static class ModuleTwo {
        /** 5 维知识基础（面积图 + 双饼图） */
        private List<KnowledgeDimension> dimensions;
        /** 双饼图数据 */
        private PieData pieA;
        private PieData pieB;
    }

    @Data
    public static class KnowledgeDimension {
        private String name;          // 维度名称
        private Integer score;        // 0-100 分数
    }

    /** 模块三：学习行为 */
    @Data
    public static class ModuleThree {
        /** 上方面积图配置 */
        private AreaConfig areaConfig;
        /** 下方多彩条形图数据 */
        private List<ColorBarItem> behaviorItems;
    }

    /** 面积图配置 */
    @Data
    public static class AreaConfig {
        private String title;
        private String color;         // 主色
        private List<String> name;    // 系列名称
        private List<AreaSeries> data;
    }

    @Data
    public static class AreaSeries {
        private String name;
        private List<String> color;   // [topColor, bottomColor]
        private List<Integer> data;
    }

    /** 彩色条图数据项 */
    @Data
    public static class ColorBarItem {
        private String name;
        private Integer value;
        private ItemStyle itemStyle;

        @Data
        public static class ItemStyle {
            private String color;
        }
    }

    /** 模块四：知识获取偏好漏斗 */
    @Data
    public static class ModuleFour {
        private List<FunnelItem> items;
    }

    @Data
    public static class FunnelItem {
        private String name;          // 偏好类型
        private Integer value;        // 偏好值 0-100
    }

    /** 模块五：AI 工具使用 */
    @Data
    public static class ModuleFive {
        private List<UsageItem> items;
    }

    @Data
    public static class UsageItem {
        private String name;          // 分类名
        private Integer value;        // 综合值
        private List<Integer> data;   // 8 个维度数据
    }

    /** Page2 学习分析 */
    @Data
    public static class LearningAnalysis {
        /** 模块一雷达：学习投入度 */
        private RadarData engagementRadar;
        /** 模块一面积图 */
        private AreaConfig engagementArea;
        /** 模块二雷达：认知风格 */
        private RadarData cognitiveRadar;
        /** 模块二图表 */
        private List<TimeSeriesPoint> cognitiveTrend;
        /** 模块三雷达：社交维度 */
        private RadarData socialRadar;
        /** 模块四雷达：内容维度 */
        private RadarData contentRadar;
        /** 中心综合分数 */
        private Integer overallScore;
    }

    /** 雷达图数据 */
    @Data
    public static class RadarData {
        private String title;
        private List<String> position;       // [left, top] e.g. ["center", "85%"]
        private List<String> center;         // [x, y] e.g. ["50%", "50%"]
        private List<RadarIndicator> indicator;
        private List<RadarSeries> data;
    }

    @Data
    public static class RadarIndicator {
        private String text;
    }

    @Data
    public static class RadarSeries {
        private String name;
        private String color;
        private List<Double> value;
    }

    /** Page2 底部模块 */
    @Data
    public static class BottomModules {
        private List<TimeSeriesPoint> module5Trend;
        private List<TimeSeriesPoint> module6Trend;
        private List<TimeSeriesPoint> module7Trend;
        private List<TimeSeriesPoint> module8Trend;
    }
}
