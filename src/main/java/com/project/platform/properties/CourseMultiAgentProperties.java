package com.project.platform.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "course.multi-agent")
public class CourseMultiAgentProperties {
    private boolean enabled = false;
    private String pythonExecutable = "py";
    private List<String> launcherArgs = new ArrayList<>(List.of("-3"));
    private String workingDirectory = "python";
    private String module = "course_multi_agent.cli";
    private int timeoutSeconds = 180;
    private String language = "zh-CN";
}
