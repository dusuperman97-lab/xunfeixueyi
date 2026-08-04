package com.project.platform.service;

import com.project.platform.vo.profile.ProfileConversationVO;

public interface ProfileConversationService {
    ProfileConversationVO converse(Integer studentId, String message);
}
