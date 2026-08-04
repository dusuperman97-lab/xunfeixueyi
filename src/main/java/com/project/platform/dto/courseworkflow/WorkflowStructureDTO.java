package com.project.platform.dto.courseworkflow;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class WorkflowStructureDTO {
    private List<StageNode> stages = new ArrayList<>();

    @Data
    public static class StageNode {
        private String stageId;
        private String stageName;
        private Integer sortOrder;
        private List<AgentNode> agents = new ArrayList<>();
    }

    @Data
    public static class AgentNode {
        private Integer id;
        private String roleCode;
        private String roleName;
        private String description;
        private String accentColor;
        private String avatar;
        private Integer sortOrder;
        private Integer isActive;
    }
}
