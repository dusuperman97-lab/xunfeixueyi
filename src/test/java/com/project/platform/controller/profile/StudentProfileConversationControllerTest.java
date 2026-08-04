package com.project.platform.controller.profile;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.profile.ProfileConversationRequest;
import com.project.platform.service.ProfileConversationService;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.profile.ProfileConversationVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StudentProfileConversationControllerTest {
    @Test
    void delegatesConversationTurnForCurrentStudent() {
        Tools tools = mock(Tools.class);
        ProfileConversationService service = mock(ProfileConversationService.class);
        when(tools.getCurrentUserIdInteger()).thenReturn(9);
        when(service.converse(9, "我想准备考研")).thenReturn(new ProfileConversationVO());
        StudentProfileGenerationController controller = new StudentProfileGenerationController();
        ReflectionTestUtils.setField(controller, "tools", tools);
        ReflectionTestUtils.setField(controller, "profileConversationService", service);

        ResponseVO<ProfileConversationVO> response = controller.conversationTurn(new ProfileConversationRequest("我想准备考研"));

        assertEquals(200, response.getCode());
        verify(service).converse(9, "我想准备考研");
    }

    @Test
    void rejectsBlankConversationTurn() {
        StudentProfileGenerationController controller = new StudentProfileGenerationController();
        ResponseVO<ProfileConversationVO> response = controller.conversationTurn(new ProfileConversationRequest("  "));
        assertEquals(400, response.getCode());
    }
}
