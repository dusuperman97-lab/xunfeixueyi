package com.project.platform.service.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.dto.courseagent.CourseMultiAgentOutlineResponse;
import com.project.platform.dto.courseagent.CourseMultiAgentSectionRequest;
import com.project.platform.dto.courseagent.CourseMultiAgentSectionResponse;
import com.project.platform.properties.CourseMultiAgentProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PythonCourseMultiAgentGateway {

    @Resource
    private CourseMultiAgentProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CourseWorkflowTraceService courseWorkflowTraceService;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public CourseMultiAgentOutlineResponse generateOutline(String message, Integer userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        payload.put("userId", userId);
        payload.put("language", properties.getLanguage());
        return execute("outline", payload, CourseMultiAgentOutlineResponse.class);
    }

    public CourseMultiAgentSectionResponse generateSection(CourseMultiAgentSectionRequest request) {
        if (request.getLanguage() == null || request.getLanguage().isBlank()) {
            request.setLanguage(properties.getLanguage());
        }
        return execute("section", request, CourseMultiAgentSectionResponse.class);
    }

    private <T> T execute(String command, Object payload, Class<T> responseType) {
        File workingDirectory = new File(properties.getWorkingDirectory());
        if (!workingDirectory.exists()) {
            throw new IllegalStateException("Python multi-agent working directory does not exist: " + workingDirectory.getAbsolutePath());
        }

        List<String> commandParts = new ArrayList<>();
        commandParts.add(properties.getPythonExecutable());
        commandParts.addAll(properties.getLauncherArgs());
        commandParts.add("-m");
        commandParts.add(properties.getModule());
        commandParts.add(command);
        ProcessBuilder builder = new ProcessBuilder(commandParts);
        builder.directory(workingDirectory);
        builder.environment().put("PYTHONUTF8", "1");
        builder.environment().put("PYTHONIOENCODING", "utf-8");

        try {
            String taskId = courseWorkflowTraceService.currentTaskId();
            Process process = builder.start();

            byte[] requestBody = objectMapper.writeValueAsBytes(payload);
            process.getOutputStream().write(requestBody);
            process.getOutputStream().flush();
            process.getOutputStream().close();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread stdoutThread = collectStream(process.getInputStream(), stdout, null, command);
            Thread stderrThread = collectStream(process.getErrorStream(), stderr, taskId, command);

            boolean finished = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            stdoutThread.join(2000L);
            stderrThread.join(2000L);

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Python multi-agent timed out after " + Duration.ofSeconds(properties.getTimeoutSeconds()));
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("Python multi-agent failed: " + stderr);
            }

            return objectMapper.readValue(stdout.toString(), responseType);
        } catch (Exception e) {
            log.warn("Python multi-agent execution failed for command={}", command, e);
            throw new IllegalStateException("Python multi-agent execution failed for command=" + command, e);
        }
    }

    private Thread collectStream(InputStream stream, StringBuilder sink, String taskId, String command) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (taskId != null && line.startsWith("PROGRESS ")) {
                        handleProgressEvent(taskId, command, line.substring("PROGRESS ".length()));
                    } else {
                        synchronized (sink) {
                            sink.append(line).append(System.lineSeparator());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to consume python process stream", e);
            }
        }, "course-multi-agent-" + command + "-stream");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void handleProgressEvent(String taskId, String command, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String event = text(node, "event");
            String stageId = text(node, "stageId");
            String detail = text(node, "detail");
            if ("start_step".equals(event)) {
                courseWorkflowTraceService.startStep(taskId, stageId, detail);
                return;
            }
            if ("complete_step".equals(event)) {
                courseWorkflowTraceService.completeStep(taskId, stageId, detail);
                return;
            }
            if ("update_title".equals(event)) {
                courseWorkflowTraceService.updateTitle(taskId, text(node, "title"), text(node, "subtitle"));
                return;
            }
            if ("activity".equals(event)) {
                courseWorkflowTraceService.logActivity(
                        taskId,
                        stageId,
                        text(node, "agentName"),
                        text(node, "agentRole"),
                        text(node, "type"),
                        text(node, "label"),
                        text(node, "message")
                );
                return;
            }
            log.debug("Ignoring unknown python progress event for command={} payload={}", command, payload);
        } catch (Exception e) {
            log.debug("Failed to parse python progress event for command={} payload={}", command, payload, e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
