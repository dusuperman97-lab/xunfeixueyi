package com.project.platform.vo.profile;

import com.project.platform.entity.StudentProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileConversationVO {
    private String reply;
    private StudentProfile profile;
    private List<Dimension> dimensions = new ArrayList<>();
    private List<Change> changes = new ArrayList<>();
    private List<String> actions = new ArrayList<>();
    private String nextQuestion;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimension {
        private String key;
        private String name;
        private Integer score;
        private String summary;
        private int confidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Change {
        private String dimension;
        private int delta;
        private String evidence;
        private int confidence;
    }
}
