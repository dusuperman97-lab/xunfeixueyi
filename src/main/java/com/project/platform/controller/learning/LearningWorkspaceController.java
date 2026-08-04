package com.project.platform.controller.learning;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.learning.LearningContextDTO;
import com.project.platform.dto.learning.SectionLearningOverviewDTO;
import com.project.platform.entity.LearningActionEvent;
import com.project.platform.entity.StudentSectionProgress;
import com.project.platform.service.LearningWorkspaceService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning-workspace")
public class LearningWorkspaceController {

    @Resource
    private LearningWorkspaceService learningWorkspaceService;

    @Resource
    private Tools tools;

    @GetMapping("/context")
    public ResponseVO<LearningContextDTO> context(@RequestParam Integer courseId,
                                                  @RequestParam Integer sectionId,
                                                  @RequestParam(required = false) String activeModule,
                                                  @RequestParam(required = false) String activeKnowledgePoint) {
        Integer studentId = tools.getCurrentUserIdInteger();
        return ResponseVO.ok(learningWorkspaceService.getContext(
                studentId, courseId, sectionId, activeModule, activeKnowledgePoint));
    }

    @GetMapping("/section/{sectionId}/overview")
    public ResponseVO<SectionLearningOverviewDTO> overview(@PathVariable Integer sectionId,
                                                           @RequestParam(required = false) Integer courseId,
                                                           @RequestParam(required = false) String activeModule,
                                                           @RequestParam(required = false) String activeKnowledgePoint) {
        Integer studentId = tools.getCurrentUserIdInteger();
        return ResponseVO.ok(learningWorkspaceService.getSectionOverview(
                studentId, courseId, sectionId, activeModule, activeKnowledgePoint));
    }

    @PostMapping("/events")
    public ResponseVO<StudentSectionProgress> recordEvent(@RequestBody LearningActionEvent event) {
        if (event.getStudentId() == null) {
            event.setStudentId(tools.getCurrentUserIdInteger());
        }
        return ResponseVO.ok(learningWorkspaceService.recordEvent(event));
    }
}
