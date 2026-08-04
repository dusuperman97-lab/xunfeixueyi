package com.project.platform.controller.learning;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.learning.LearningAssistantContextDTO;
import com.project.platform.dto.learning.LearningAssistantContextRequest;
import com.project.platform.entity.LearningActionEvent;
import com.project.platform.entity.StudentSectionProgress;
import com.project.platform.service.LearningAssistantService;
import com.project.platform.service.LearningWorkspaceService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning-assistant")
public class LearningAssistantController {

    @Resource
    private Tools tools;

    @Resource
    private LearningAssistantService learningAssistantService;

    @Resource
    private LearningWorkspaceService learningWorkspaceService;

    @GetMapping("/context")
    public ResponseVO<LearningAssistantContextDTO> context(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer sectionId,
            @RequestParam(required = false) String activeModule,
            @RequestParam(required = false) String activeKnowledgePoint) {
        LearningAssistantContextRequest request = new LearningAssistantContextRequest();
        request.setCourseId(courseId);
        request.setSectionId(sectionId);
        request.setActiveModule(activeModule);
        request.setActiveKnowledgePoint(activeKnowledgePoint);
        return ResponseVO.ok(learningAssistantService.getContext(tools.getCurrentUserIdInteger(), request));
    }

    @PostMapping("/events")
    public ResponseVO<StudentSectionProgress> recordEvent(@RequestBody LearningActionEvent event) {
        event.setStudentId(tools.getCurrentUserIdInteger());
        if (event.getTriggerSource() == null || event.getTriggerSource().isBlank()) {
            event.setTriggerSource("study-assistant");
        }
        return ResponseVO.ok(learningWorkspaceService.recordEvent(event));
    }
}
