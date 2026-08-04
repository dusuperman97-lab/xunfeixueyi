package com.project.platform.service;

import com.project.platform.entity.StudentProfile;
import com.project.platform.service.impl.ProfileConversationServiceImpl;
import com.project.platform.vo.profile.ProfileConversationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileConversationServiceImplTest {

    private StudentProfileService profileService;
    private ProfileConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        profileService = mock(StudentProfileService.class);
        when(profileService.getProfileByStudentId(7)).thenReturn(null);
        when(profileService.saveOrUpdateProfile(any())).thenAnswer(invocation -> {
            StudentProfile profile = invocation.getArgument(0);
            profile.setVersion(1);
            return profile;
        });
        service = new ProfileConversationServiceImpl(profileService);
    }

    @Test
    void extractsMajorGoalPreferencesAndEvidenceFromNaturalLanguage() {
        ProfileConversationVO result = service.converse(7,
                "我是计算机专业大二学生，目标是考研。我喜欢看视频和结合例题学习，但高等数学函数总是容易算错。");

        assertEquals("计算机", result.getProfile().getMajor());
        assertTrue(result.getProfile().getLearningGoal().contains("考研"));
        assertNotNull(result.getProfile().getVideoPreference());
        assertNotNull(result.getProfile().getExamplePreference());
        assertNull(result.getProfile().getTextPreference());
        assertFalse(result.getChanges().isEmpty());
        assertTrue(result.getChanges().stream().allMatch(change -> change.getEvidence() != null));
        assertEquals(8, result.getDimensions().size());
        assertTrue(result.getActions().size() >= 3);
    }

    @Test
    void incrementsExistingProfileWithoutExceedingScoreRange() {
        StudentProfile existing = new StudentProfile();
        existing.setStudentId(7);
        existing.setVersion(4);
        existing.setLearningInitiative(98);
        when(profileService.getProfileByStudentId(7)).thenReturn(existing);

        ProfileConversationVO result = service.converse(7, "我每天都会主动复盘并制定学习计划，坚持练习两小时。");

        assertTrue(result.getProfile().getLearningInitiative() <= 100);
        assertTrue(result.getProfile().getTaskManagement() <= 100);
        verify(profileService).saveOrUpdateProfile(existing);
    }

    @Test
    void rejectsBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> service.converse(7, "  "));
        verify(profileService, never()).saveOrUpdateProfile(any());
    }

    @Test
    void leavesUnobservedDimensionsUnknownInsteadOfInventingScores() {
        ProfileConversationVO result = service.converse(7, "我的目标是考研");

        assertNull(result.getProfile().getConceptClarity());
        assertNull(result.getProfile().getTransferAbility());
        assertNull(result.getDimensions().stream()
                .filter(item -> item.getKey().equals("foundation"))
                .findFirst().orElseThrow().getScore());
        assertEquals(0, result.getDimensions().stream()
                .filter(item -> item.getKey().equals("foundation"))
                .findFirst().orElseThrow().getConfidence());
    }

    @Test
    void doesNotForceUnrecognizedConversationIntoKnowledgeFoundation() {
        ProfileConversationVO result = service.converse(7, "最近学习有点乱，不知道该从哪里和你说起");

        assertTrue(result.getChanges().isEmpty());
        assertTrue(result.getReply().contains("还不能可靠判断"));
        verify(profileService, never()).saveOrUpdateProfile(any());
    }

    @Test
    void oneNaturalSentenceCanUpdateSeveralDifferentDimensions() {
        ProfileConversationVO result = service.converse(7,
                "我通常晚上学两小时，先看视频再做例题，每周会复盘错题，希望三个月后参加竞赛。");

        assertTrue(result.getChanges().stream().anyMatch(item -> item.getDimension().equals("学习节奏")));
        assertTrue(result.getChanges().stream().anyMatch(item -> item.getDimension().equals("内容偏好")));
        assertTrue(result.getChanges().stream().anyMatch(item -> item.getDimension().equals("易错模式")));
        assertTrue(result.getChanges().stream().anyMatch(item -> item.getDimension().equals("学习目标")));
    }
}
