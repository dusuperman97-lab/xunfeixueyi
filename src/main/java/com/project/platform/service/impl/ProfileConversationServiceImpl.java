package com.project.platform.service.impl;

import com.alibaba.fastjson2.JSON;
import com.project.platform.entity.StudentProfile;
import com.project.platform.service.ProfileConversationService;
import com.project.platform.service.StudentProfileService;
import com.project.platform.vo.profile.ProfileConversationVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProfileConversationServiceImpl implements ProfileConversationService {
    private static final Pattern MAJOR = Pattern.compile("(?:我是|学|读)([\\u4e00-\\u9fa5A-Za-z]{2,16})(?:专业|系)");
    private final StudentProfileService profileService;

    public ProfileConversationServiceImpl(StudentProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public ProfileConversationVO converse(Integer studentId, String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) throw new IllegalArgumentException("请说说你的学习情况");
        if (message.length() > 1000) throw new IllegalArgumentException("单次描述请控制在1000字以内");

        StudentProfile profile = profileService.getProfileByStudentId(studentId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setStudentId(studentId);
        }
        List<ProfileConversationVO.Change> changes = new ArrayList<>();
        Matcher major = MAJOR.matcher(message);
        if (major.find()) {
            profile.setMajor(major.group(1));
            change(changes, "专业背景", 12, message, 96);
        }
        String goal = firstMatch(message, "考研", "保研", "就业", "考公", "竞赛", "四六级", "转专业", "出国");
        if (goal != null) {
            profile.setLearningGoal(goal + "，并形成可执行的阶段计划");
            change(changes, "学习目标", 16, message, 94);
        }
        if (containsAny(message, "视频", "讲解", "动画", "听课", "网课")) {
            profile.setVideoPreference(increase(profile.getVideoPreference(), 72, 10));
            change(changes, "内容偏好", 18, message, 92);
        }
        if (containsAny(message, "例题", "案例", "动手", "实践", "刷题", "做题")) {
            profile.setExamplePreference(increase(profile.getExamplePreference(), 72, 10));
            profile.setPracticeOrientation(increase(profile.getPracticeOrientation(), 68, 8));
            change(changes, "认知风格", 10, message, 90);
        }
        if (containsAny(message, "容易错", "总是错", "不会", "薄弱", "困难", "记不住", "错题", "做错", "粗心")) {
            profile.setErrorPreferences(JSON.toJSONString(List.of(message)));
            profile.setErrorTypeCount(Math.max(1, profile.getErrorTypeCount() == null ? 1 : profile.getErrorTypeCount()));
            change(changes, "易错模式", 14, message, 91);
        }
        if (containsAny(message, "每天", "坚持", "主动", "复盘", "每周", "总结", "预习")) {
            profile.setLearningInitiative(increase(profile.getLearningInitiative(), 70, 8));
            change(changes, "学习主动性", 12, message, 91);
        }
        if (containsAny(message, "计划", "安排", "小时", "分钟", "早上", "晚上", "周末", "时间")) {
            profile.setTaskManagement(increase(profile.getTaskManagement(), 68, 8));
            change(changes, "学习节奏", 10, message, 88);
        }
        if (containsAny(message, "举一反三", "应用", "项目", "解决问题", "实战", "迁移")) {
            profile.setTransferAbility(increase(profile.getTransferAbility(), 68, 8));
            change(changes, "迁移能力", 12, message, 88);
        }
        if (containsAny(message, "零基础", "完全不懂", "刚开始学", "基础薄弱")) {
            profile.setConceptClarity(30);
            profile.setKnowledgeDepth(25);
            change(changes, "知识基础", -12, message, 90);
        } else if (containsAny(message, "已经掌握", "基础扎实", "很熟悉", "系统学过")) {
            profile.setConceptClarity(78);
            profile.setKnowledgeDepth(76);
            change(changes, "知识基础", 12, message, 88);
        }

        StudentProfile saved = changes.isEmpty() ? profile : profileService.saveOrUpdateProfile(profile);
        List<ProfileConversationVO.Dimension> dimensions = dimensions(saved, changes);
        List<String> actions = actions(saved);
        String next = nextQuestion(saved);
        String reply = changes.isEmpty()
                ? "这句话还不能可靠判断你的画像，我不会把它硬归类。回答下面这个具体问题会更有帮助。"
                : "我从这段话里识别出 " + changes.size() + " 项画像变化，每项都保留了你的原话依据。";
        return new ProfileConversationVO(
                reply,
                saved, dimensions, changes, actions, next, LocalDateTime.now());
    }

    private List<ProfileConversationVO.Dimension> dimensions(StudentProfile p, List<ProfileConversationVO.Change> changes) {
        return List.of(
                observedDim("foundation", "知识基础", nullableAverage(p.getConceptClarity(), p.getKnowledgeDepth()), "概念理解与知识深度", changes),
                observedDim("cognition", "认知风格", p.getPracticeOrientation(), "信息加工与实践倾向", changes),
                observedDim("goal", "学习目标", null, p.getLearningGoal() == null ? "等待你说出目标" : p.getLearningGoal(), changes),
                observedDim("rhythm", "学习节奏", p.getTaskManagement(), "计划与时间管理", changes),
                observedDim("preference", "内容偏好", maxNullable(p.getTextPreference(), p.getImagePreference(), p.getExamplePreference(), p.getVideoPreference()), preferenceSummary(p), changes),
                observedDim("errors", "易错模式", null, p.getErrorTypeCount() == null ? "还需要更多练习证据" : "已记录具体易错原句", changes),
                observedDim("initiative", "学习主动性", p.getLearningInitiative(), "主动规划与复盘", changes),
                observedDim("transfer", "迁移能力", p.getTransferAbility(), "把知识用于新问题", changes));
    }

    private List<String> actions(StudentProfile p) {
        List<String> result = new ArrayList<>();
        if (p.getErrorTypeCount() != null) result.add("用 15 分钟重做最近的易错题，并写下一句错误原因");
        if (p.getVideoPreference() != null && (p.getTextPreference() == null || p.getVideoPreference() >= p.getTextPreference())) result.add("先看一个 8 分钟核心概念讲解，再用一道例题检验理解");
        else result.add("先读一页核心概念摘要，再合上材料复述关键点");
        result.add(p.getLearningGoal() == null ? "告诉学习罗盘你最近最想解决的目标" : "把“" + p.getLearningGoal().split("，")[0] + "”拆成今天可完成的一个小任务");
        result.add("学习结束后用一句话复盘：我会了什么，还有哪里卡住");
        return result;
    }

    private String nextQuestion(StudentProfile p) {
        if (p.getMajor() == null) return "你现在学什么专业或方向？哪门课最让你费劲？";
        if (p.getLearningGoal() == null) return "未来三个月，你最想达成的学习目标是什么？";
        if (p.getErrorTypeCount() == null) return "最近哪类题最容易错？通常错在理解、记忆还是计算？";
        return "你更喜欢看讲解、读文字，还是先做例题再总结？为什么？";
    }

    private String preferenceSummary(StudentProfile p) {
        Integer max = maxNullable(p.getTextPreference(), p.getImagePreference(), p.getExamplePreference(), p.getVideoPreference());
        if (max == null) return "等待你描述喜欢的学习方式";
        if (max.equals(p.getExamplePreference())) return "偏好从例题和实践中理解";
        if (max.equals(p.getVideoPreference())) return "偏好视频与动态讲解";
        if (max.equals(p.getImagePreference())) return "偏好图示与结构化表达";
        return "偏好文字阅读与归纳";
    }

    private ProfileConversationVO.Dimension observedDim(String key, String name, Integer score, String summary, List<ProfileConversationVO.Change> changes) {
        ProfileConversationVO.Change evidence = changes.stream().filter(item -> item.getDimension().equals(name)).findFirst().orElse(null);
        return new ProfileConversationVO.Dimension(key, name, evidence == null ? null : score, summary, evidence == null ? 0 : evidence.getConfidence());
    }
    private Integer nullableAverage(Integer a, Integer b) { return a == null || b == null ? null : (a + b) / 2; }
    private Integer maxNullable(Integer... values) { Integer max = null; for (Integer value : values) if (value != null && (max == null || value > max)) max = value; return max; }
    private int increase(Integer current, int firstValue, int delta) { return current == null ? firstValue : clamp(current + delta); }
    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private void change(List<ProfileConversationVO.Change> list, String name, int delta, String evidence, int confidence) {
        list.add(new ProfileConversationVO.Change(name, delta, evidence, confidence));
    }
    private boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
    private String firstMatch(String text, String... words) {
        for (String word : words) if (text.contains(word)) return word;
        return null;
    }
}
