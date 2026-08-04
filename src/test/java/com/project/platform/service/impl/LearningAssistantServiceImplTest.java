package com.project.platform.service.impl;

import com.project.platform.dto.learning.LearningAssistantContextDTO;
import com.project.platform.dto.learning.LearningAssistantContextRequest;
import com.project.platform.dto.learning.LearningContextDTO;
import com.project.platform.dto.learning.RecommendedActionDTO;
import com.project.platform.entity.ResourcePush;
import com.project.platform.entity.StudentProfile;
import com.project.platform.service.LearningWorkspaceService;
import com.project.platform.service.ResourcePushService;
import com.project.platform.service.StudentProfileService;
import com.project.platform.vo.StudentProfileDashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningAssistantServiceImplTest {

    @Mock
    LearningWorkspaceService learningWorkspaceService;

    @Mock
    StudentProfileService studentProfileService;

    @Mock
    ResourcePushService resourcePushService;

    @Test
    void buildsFullContextFromLearningProgressProfileAndResources() {
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(
                learningWorkspaceService, studentProfileService, resourcePushService);

        LearningAssistantContextRequest request = new LearningAssistantContextRequest();
        request.setCourseId(1);
        request.setSectionId(3);
        request.setActiveModule("learning");
        request.setActiveKnowledgePoint("动态规划");

        LearningContextDTO learningContext = new LearningContextDTO();
        learningContext.setCourseId(1);
        learningContext.setCourseName("算法");
        learningContext.setChapterId(2);
        learningContext.setChapterName("递推");
        learningContext.setSectionId(3);
        learningContext.setSectionName("动态规划入门");
        learningContext.setActiveModule("learning");
        learningContext.setActiveKnowledgePoint("动态规划");
        learningContext.setWeakKnowledgePoints(List.of("状态转移"));
        learningContext.setNextRecommendedAction(new RecommendedActionDTO(
                "REMEDIAL_RESOURCE", "查看补救资源", "resource", 3, "状态转移"));

        StudentProfile profile = new StudentProfile();
        profile.setLearningGoal("通过算法考试");
        profile.setPreferredStyle("视频示例");
        profile.setWeaknessPoints("[\"边界条件\"]");

        StudentProfileDashboardVO dashboard = new StudentProfileDashboardVO();
        StudentProfileDashboardVO.StatsOverview stats = new StudentProfileDashboardVO.StatsOverview();
        stats.setOverallScore(82);
        stats.setStudyMinutes(45);
        stats.setConcentration(58);
        stats.setKnowledgeMastery(76);
        stats.setRiskWarning("专注度波动");
        dashboard.setStatsOverview(stats);

        ResourcePush resource = new ResourcePush();
        resource.setId(11);
        resource.setTitle("状态转移专项练习");

        when(learningWorkspaceService.getContext(99, 1, 3, "learning", "动态规划")).thenReturn(learningContext);
        when(resourcePushService.listContextualResources(99, 1, 2, 3, "动态规划")).thenReturn(List.of(resource));
        when(studentProfileService.getProfileByStudentId(99)).thenReturn(profile);
        when(studentProfileService.getDashboard(99)).thenReturn(dashboard);

        LearningAssistantContextDTO result = service.getContext(99, request);

        assertTrue(result.isLearningContextAvailable());
        assertEquals("FULL_CONTEXT", result.getContextStatus());
        assertEquals("正在学习：动态规划入门", result.getGreeting());
        assertEquals(List.of("状态转移", "边界条件"), result.getWeakKnowledgePoints());
        assertEquals(1, result.getContextualResources().size());
        assertEquals("看画像", result.getActions().get(result.getActions().size() - 1).getLabel());
        assertTrue(result.getTips().stream().anyMatch(tip -> tip.getText().contains("状态转移")));
        assertEquals(82, result.getProfileSummary().getOverallScore());
    }

    @Test
    void stillReturnsProfileDrivenActionsWhenNoCourseSectionContextExists() {
        LearningAssistantServiceImpl service = new LearningAssistantServiceImpl(
                learningWorkspaceService, studentProfileService, resourcePushService);

        StudentProfile profile = new StudentProfile();
        profile.setLearningGoal("通过项目掌握 Java");
        profile.setLearningStyle("项目实践");
        profile.setPreferredStyle("示例驱动");
        profile.setWeaknessPoints("[\"polymorphism\"]");

        StudentProfileDashboardVO dashboard = new StudentProfileDashboardVO();
        StudentProfileDashboardVO.StatsOverview stats = new StudentProfileDashboardVO.StatsOverview();
        stats.setOverallScore(72);
        stats.setStudyMinutes(45);
        stats.setConcentration(83);
        stats.setKnowledgeMastery(68);
        stats.setRiskWarning("正常");
        dashboard.setStatsOverview(stats);

        when(studentProfileService.getProfileByStudentId(101)).thenReturn(profile);
        when(studentProfileService.getDashboard(101)).thenReturn(dashboard);

        LearningAssistantContextDTO result =
                service.getContext(101, new LearningAssistantContextRequest());

        assertFalse(result.isLearningContextAvailable());
        assertEquals("通过项目掌握 Java", result.getProfileSummary().getLearningGoal());
        assertNotNull(result.getTips());
        assertTrue(result.getActions().stream().anyMatch(action ->
                "PROFILE_DASHBOARD".equals(action.getActionType())
                        && "/student/new-profile".equals(action.getRoute())));
        verify(learningWorkspaceService, never()).getContext(any(), any(), any(), any(), any());
    }
}
