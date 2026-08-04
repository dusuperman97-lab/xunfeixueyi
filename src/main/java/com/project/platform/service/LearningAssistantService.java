package com.project.platform.service;

import com.project.platform.dto.learning.LearningAssistantContextDTO;
import com.project.platform.dto.learning.LearningAssistantContextRequest;

public interface LearningAssistantService {

    LearningAssistantContextDTO getContext(Integer studentId, LearningAssistantContextRequest request);
}
