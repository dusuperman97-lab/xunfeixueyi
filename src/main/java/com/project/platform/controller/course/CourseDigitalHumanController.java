package com.project.platform.controller.course;

import com.project.platform.agents.config.mongdb.PersistentChatMemoryStore;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.dto.AvatarTicketResponse;
import com.project.platform.dto.CourseDigitalHumanChatRequest;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.service.course.AvatarTicketService;
import com.project.platform.service.course.CourseDigitalHumanContextService;
import com.project.platform.vo.ResponseVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/course/digital-human")
@Slf4j
public class CourseDigitalHumanController {

    private static final long SSE_TIMEOUT_MILLIS = 300_000L;
    private static final String SYSTEM_PROMPT = "你是课程中的数字人教师。必须依据用户消息中给出的课程上下文回答；不知道时明确说明，不编造讲义内容。回答使用中文，适合直接朗读，结构清晰且避免冗长。";

    private final CourseDigitalHumanContextService contextService;
    private final StreamingChatModel qwenStreamingChatModel;
    private final PersistentChatMemoryStore chatMemoryStore;
    private final AvatarTicketService avatarTicketService;
    private final Tools tools;

    public CourseDigitalHumanController(CourseDigitalHumanContextService contextService,
                                        @Qualifier("qwenStreamingChatModel") StreamingChatModel qwenStreamingChatModel,
                                        PersistentChatMemoryStore chatMemoryStore,
                                        AvatarTicketService avatarTicketService,
                                        Tools tools) {
        this.contextService = contextService;
        this.qwenStreamingChatModel = qwenStreamingChatModel;
        this.chatMemoryStore = chatMemoryStore;
        this.avatarTicketService = avatarTicketService;
        this.tools = tools;
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @TrackAiUsage(AiToolUsage.ToolType.VIRTUAL_TEACHER)
    public SseEmitter chat(@RequestBody CourseDigitalHumanChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        emitter.onCompletion(() -> dispose(subscription));
        emitter.onTimeout(() -> dispose(subscription));
        emitter.onError(error -> dispose(subscription));

        try {
            CourseDigitalHumanContextService.Context context = contextService.build(request);
            String memoryId = resolveMemoryId(request.getMemoryId(), context);
            send(emitter, "meta", Map.of(
                    "memoryId", memoryId,
                    "type", request.getType().name(),
                    "courseId", context.getCourse().getId(),
                    "chapterId", context.getChapter().getId(),
                    "sectionId", context.getSection().getId(),
                    "digitalHumanId", context.getDigitalHuman().getDigitalHumanId()));

            List<ChatMessage> history = chatMemoryStore.getMessages(memoryId);
            List<ChatMessage> modelMessages = new ArrayList<>();
            modelMessages.add(SystemMessage.from(SYSTEM_PROMPT));
            modelMessages.addAll(history);
            modelMessages.add(UserMessage.from(context.getPrompt()));

            subscription.set(Flux.<String>create(sink -> {
                StringBuilder fullResponse = new StringBuilder();
                qwenStreamingChatModel.chat(modelMessages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        sink.next(token);
                        fullResponse.append(token);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        try {
                            List<ChatMessage> toSave = new ArrayList<>(history);
                            toSave.add(UserMessage.from(context.getPrompt()));
                            toSave.add(AiMessage.from(fullResponse.toString()));
                            chatMemoryStore.updateMessages(memoryId, toSave);
                            log.info("=== 数字人Memory更新完成, memoryId={}", memoryId);
                        } catch (Exception e) {
                            log.error("=== 更新数字人Memory失败", e);
                        }
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("=== 数字人流式响应错误", error);
                        sink.error(error);
                    }
                });
            }).subscribe(
                    delta -> sendOrFail(emitter, "delta", Map.of("content", delta)),
                    error -> finishWithError(emitter, error),
                    () -> {
                        sendOrFail(emitter, "done", Map.of("memoryId", memoryId));
                        emitter.complete();
                    }));
        } catch (Exception error) {
            finishWithError(emitter, error);
        }
        return emitter;
    }

    @GetMapping("/avatar-ticket")
    public ResponseVO<AvatarTicketResponse> avatarTicket(@RequestParam String digitalHumanId) {
        return ResponseVO.ok(avatarTicketService.create(digitalHumanId));
    }

    private String resolveMemoryId(String supplied, CourseDigitalHumanContextService.Context context) {
        String userId = tools.getCurrentUserId();
        if (supplied != null && !supplied.isBlank()) {
            String memoryId = supplied.trim();
            if (!memoryId.startsWith(userId + "_course_avatar_")) {
                throw new IllegalArgumentException("memoryId 不属于当前用户的课程数字人会话");
            }
            return memoryId;
        }
        return userId + "_course_avatar_"
                + context.getCourse().getId() + "_" + context.getSection().getId() + "_"
                + UUID.randomUUID();
    }

    private static void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
    }

    private static void sendOrFail(SseEmitter emitter, String event, Object data) {
        try {
            send(emitter, event, data);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private static void finishWithError(SseEmitter emitter, Throwable error) {
        log.error("课程数字人流式对话失败", error);
        try {
            send(emitter, "error", Map.of("message", safeMessage(error)));
            emitter.complete();
        } catch (IOException sendError) {
            emitter.completeWithError(sendError);
        }
    }

    private static String safeMessage(Throwable error) {
        if (error instanceof IllegalArgumentException && error.getMessage() != null) {
            return error.getMessage();
        }
        return "数字人回答失败，请稍后重试";
    }

    private static void dispose(AtomicReference<Disposable> subscription) {
        Disposable disposable = subscription.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
