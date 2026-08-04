package com.project.platform.service.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WorkflowSseEmitterService {

    @Resource
    private ObjectMapper objectMapper;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String taskId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.put(taskId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE 客户端断开: taskId={}", taskId);
            emitters.remove(taskId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE 超时: taskId={}", taskId);
            emitters.remove(taskId);
        });
        emitter.onError(e -> {
            log.warn("SSE 错误: taskId={}, error={}", taskId, e.getMessage());
            emitters.remove(taskId);
        });

        return emitter;
    }

    public void send(String taskId, String event, Object payload) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) return;

        try {
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event()
                    .name(event)
                    .data(json));
        } catch (IOException e) {
            log.warn("SSE 发送失败: taskId={}, event={}", taskId, event);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
            emitters.remove(taskId);
        }
    }

    public void sendSnapshot(String taskId, CourseWorkflowSnapshotDTO snapshot) {
        send(taskId, "snapshot", snapshot);
    }

    public void sendStageStart(String taskId, String stageId, String detail) {
        send(taskId, "stage_start", Map.of("stageId", stageId, "detail", detail));
    }

    public void sendStageComplete(String taskId, String stageId, String detail) {
        send(taskId, "stage_complete", Map.of("stageId", stageId, "detail", detail));
    }

    public void sendActivity(String taskId, CourseWorkflowSnapshotDTO.Activity activity) {
        send(taskId, "activity", activity);
    }

    public void sendEvidence(String taskId, CourseWorkflowSnapshotDTO.Evidence evidence) {
        send(taskId, "evidence_added", evidence);
    }

    public void sendReviewRequired(String taskId,
                                    List<CourseWorkflowSnapshotDTO.Evidence> resources) {
        send(taskId, "review_required", Map.of("resources", resources));
    }

    public void sendWorkflowComplete(String taskId, String summary, Integer courseId,
                                       String courseName) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("summary", summary);
        if (courseId != null) data.put("courseId", courseId);
        if (courseName != null) data.put("courseName", courseName);
        send(taskId, "workflow_complete", data);
    }

    public void sendWorkflowFailed(String taskId, String summary) {
        send(taskId, "workflow_failed", Map.of("summary", summary));
    }

    public void close(String taskId) {
        SseEmitter emitter = emitters.remove(taskId);
        if (emitter != null) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }

    public boolean hasConnection(String taskId) {
        return emitters.containsKey(taskId);
    }
}
