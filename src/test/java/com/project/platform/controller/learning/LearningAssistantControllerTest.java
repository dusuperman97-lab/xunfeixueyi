package com.project.platform.controller.learning;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.LearningActionEvent;
import com.project.platform.entity.StudentSectionProgress;
import com.project.platform.service.LearningAssistantService;
import com.project.platform.service.LearningWorkspaceService;
import com.project.platform.vo.ResponseVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningAssistantControllerTest {

    @Test
    void recordEventAlwaysUsesCurrentStudentInsteadOfClientStudentId() {
        Tools tools = mock(Tools.class);
        LearningAssistantService assistantService = mock(LearningAssistantService.class);
        LearningWorkspaceService workspaceService = mock(LearningWorkspaceService.class);
        StudentSectionProgress progress = StudentSectionProgress.create(9, 10, null, 30);
        when(tools.getCurrentUserIdInteger()).thenReturn(9);
        when(workspaceService.recordEvent(any(LearningActionEvent.class))).thenReturn(progress);
        LearningAssistantController controller = new LearningAssistantController();
        ReflectionTestUtils.setField(controller, "tools", tools);
        ReflectionTestUtils.setField(controller, "learningAssistantService", assistantService);
        ReflectionTestUtils.setField(controller, "learningWorkspaceService", workspaceService);

        LearningActionEvent event = new LearningActionEvent();
        event.setStudentId(999);
        event.setCourseId(10);
        event.setSectionId(30);
        event.setEventType("AI_EXPLANATION_REQUESTED");

        ResponseVO<StudentSectionProgress> response = controller.recordEvent(event);

        ArgumentCaptor<LearningActionEvent> captor = ArgumentCaptor.forClass(LearningActionEvent.class);
        verify(workspaceService).recordEvent(captor.capture());
        assertEquals(200, response.getCode());
        assertEquals(9, captor.getValue().getStudentId());
        assertEquals("study-assistant", captor.getValue().getTriggerSource());
    }
}
