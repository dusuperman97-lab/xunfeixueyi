package com.project.platform.controller.chat;

import com.project.platform.agents.ai.AiCodeHelperService;
import com.project.platform.agents.ai.SparkAiCodeHelperService;
import com.project.platform.agents.config.utils.ChatMemoryTools;
import com.project.platform.agents.config.utils.MultimodalFileHandler;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.entry.ChatForm;
import com.project.platform.agents.entry.ChatMessages;
import com.project.platform.annotation.ConsumePoints;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.dto.ChatMessageDTO;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.service.AiToolUsageService;
import com.project.platform.service.ChatSessionService;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.SessionVO;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天控制器
 * 处理与AI的对话交互，支持多模态（文本、图片、文档）
 */
@RestController
@RequestMapping("/xunfei/chat")
@Slf4j
public class ChatController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private SparkAiCodeHelperService sparkAiCodeHelperService;

    @Resource
    private Tools tools;

    @Resource
    private ChatMemoryTools chatMemoryTools;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private MultimodalFileHandler multimodalFileHandler;

    @Resource
    private AiToolUsageService aiToolUsageService;

    /**
     * 流式聊天接口(带会话记忆)
     * 支持多模态:文本、图片、文档
     */
    @PostMapping("/qwen")
    @TrackAiUsage(AiToolUsage.ToolType.CHAT)
    @ConsumePoints(value = 2, bizType = "CHAT")
    public Flux<String> qwenChat(@ModelAttribute ChatForm chatForm,
                                 @RequestParam(value = "images", required = false) MultipartFile[] images,
                                 @RequestParam(value = "documents", required = false) MultipartFile[] documents,
                                 HttpServletResponse response) {
        String memoryId = chatForm.getMemoryId();
        Integer userId = tools.getCurrentUserIdInteger();

        // 如果没有传入 memoryId，生成新的会话ID
        if (memoryId == null || memoryId.isEmpty()) {
            memoryId = userId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            log.info("=== 生成新会话ID: {}", memoryId);
            // 将 memoryId 放到响应头中返回给前端
            response.setHeader("X-Conversation-Id", memoryId);
            response.setHeader("Content-Type","text/plain; charset=UTF-8");
            response.setHeader("Cache-Control","no-cache");
        } else {
            log.info("=== 使用已有会话ID: {}", memoryId);
        }

        log.info("=== 聊天被调用！userId: {}, memoryId: {}, 用户输入：{}", userId, memoryId, chatForm.getMessage());

        // 处理上传的图片
        List<String> uploadedImageUrls = new ArrayList<>();
        if (images != null && images.length > 0) {
            log.info("=== 上传图片：{}", images.length);
            for (MultipartFile image : images) {
                if (!image.isEmpty() && MultimodalFileHandler.isSupportedImage(image.getOriginalFilename())) {
                    String imageUrl = multimodalFileHandler.saveImage(image);
                    uploadedImageUrls.add(imageUrl);
                }
            }
        }

        // 处理上传的文档
        List<String> uploadedDocUrls = new ArrayList<>();
        if (documents != null && documents.length > 0) {
            log.info("=== 上传文档：{}", documents.length);
            for (MultipartFile doc : documents) {
                if (!doc.isEmpty() && MultimodalFileHandler.isSupportedDocument(doc.getOriginalFilename())) {
                    String docUrl = multimodalFileHandler.saveDocument(doc);
                    uploadedDocUrls.add(docUrl);
                }
            }
        }

        // 如果有多模态内容，保存到数据库的记忆记录中
        boolean hasMultimodalContent = !uploadedImageUrls.isEmpty() || !uploadedDocUrls.isEmpty();
        if (hasMultimodalContent) {
            chatMemoryTools.saveMultimodalUrlsToSession(memoryId, userId.toString(), uploadedImageUrls, uploadedDocUrls);
        }

        if (hasMultimodalContent) {
            // 有多模态内容：构建LangChain4j UserMessage对象
            log.info("=== 检测到多模态内容，图片数量: {}, 文档数量: {}", uploadedImageUrls.size(), uploadedDocUrls.size());

            UserMessage userMessage = multimodalFileHandler.buildMultimodalUserMessage(
                    chatForm.getMessage(),
                    uploadedImageUrls,
                    uploadedDocUrls
            );
            log.info("使用了讯飞星火大模型");
            return sparkAiCodeHelperService.streamingChatWithMultimodal(memoryId,userMessage);
        } else {
            // 纯文本消息：使用原有方法
            return aiCodeHelperService.streamingChat(memoryId, chatForm.getMessage());
        }
    }

    /**
     * 纯文本聊天接口（不支持图片/文档）
     */
    @PostMapping("/spark")
    @TrackAiUsage(AiToolUsage.ToolType.CHAT)
    @ConsumePoints(value = 2, bizType = "CHAT")
    public Flux<String> sparkChat(@RequestBody ChatForm chatForm,HttpServletResponse response) {
        String memoryId = chatForm.getMemoryId();
        String userId = tools.getCurrentUserId();

        // 如果没有传入 memoryId，生成新的会话ID
        if (memoryId == null || memoryId.isEmpty()) {
            memoryId = userId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            log.info("=== 生成新会话ID: {}", memoryId);
            // 将 memoryId 放到响应头中返回给前端
            response.setHeader("X-Conversation-Id", memoryId);
            response.setHeader("Content-Type","text/plain; charset=UTF-8");
            response.setHeader("Cache-Control","no-cache");
        } else {
            log.info("=== 使用已有会话ID: {}", memoryId);
        }

        log.info("=== 聊天被调用！userId: {}, memoryId: {}, 用户输入：{}", userId, memoryId, chatForm.getMessage());

        // 纯文本消息：直接调用流式聊天
        return sparkAiCodeHelperService.streamingChat(memoryId, chatForm.getMessage());
    }


    /**
     * 学生画像引导式对话接口（流式）
     * 用于注册登录时与学生进行自然对话，收集学生信息
     * 聊天记录会自动保存到MongoDB中
     */
    @PostMapping("/student-profile/chat")
    @TrackAiUsage(AiToolUsage.ToolType.CHAT)
    public Flux<String> studentProfileChat(@RequestBody ChatForm chatForm, HttpServletResponse response) {
        String memoryId = chatForm.getMemoryId();
        Integer userId = tools.getCurrentUserIdInteger();

        // 如果没有传入 memoryId，生成新的会话ID（格式: userId_profile_UUID）
        if (memoryId == null || memoryId.isEmpty()) {
            memoryId = userId + "_profile_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            log.info("=== 生成新的画像引导会话ID: {}", memoryId);
            // 将 memoryId 放到响应头中返回给前端
            response.setHeader("X-Conversation-Id", memoryId);
            response.setHeader("Content-Type","text/plain; charset=UTF-8");
            response.setHeader("Cache-Control","no-cache");
        } else {
            log.info("=== 使用已有画像引导会话ID: {}", memoryId);
        }
        log.info("=== 学生画像引导对话！userId: {}, memoryId: {}, 用户输入：{}", userId, memoryId, chatForm.getMessage());

        // 使用aiCodeHelperService进行流式对话
        return aiCodeHelperService.streamingChatWithProfile(memoryId, chatForm.getMessage());
    }

    /**
     * 获取用户的所有历史会话列表
     */
    @GetMapping("/sessions")
    public ResponseVO<List<SessionVO>> getSessions() {
        String userId = tools.getCurrentUserId();
        log.info("=== 获取用户历史会话列表，userId: {}", userId);

        List<ChatMessages> sessions = chatSessionService.getAllSessions(userId);
        log.info("=== 获取用户历史会话列表成功，数量为: {}", sessions.size());

        List<SessionVO> sessionVOs = sessions.stream()
                .map(session -> new SessionVO(
                        session.getMemoryId(),
                        session.getSessionTitle(),
                        session.getCreateTime(),
                        session.getExpirationTime()
                ))
                .collect(Collectors.toList());

        return ResponseVO.ok(sessionVOs);
    }

    /**
     * 获取会话消息内容
     */
    @GetMapping("/messages")
    public ResponseVO<List<ChatMessageDTO>> getSessionMessages(@RequestParam String memoryId) {
        String userId = tools.getCurrentUserId();
        log.info("=== 获取会话消息，userId: {}, memoryId: {}", userId, memoryId);

        ChatMessages chatMessages = chatSessionService.findByMemoryIdAndUserId(memoryId, userId);

        if (chatMessages == null) {
            log.warn("=== 会话不存在或无权访问，memoryId: {}, userId: {}", memoryId, userId);
            return ResponseVO.ok(new ArrayList<>());
        }

        List<ChatMessage> messages = chatSessionService.getSessionMessages(memoryId, userId);

        // 获取该会话的所有图片和文档URL
        List<String> sessionImageUrls = chatMessages.getImagUrls() != null ? chatMessages.getImagUrls() : new ArrayList<>();
        List<String> sessionDocUrls = chatMessages.getDocUrls() != null ? chatMessages.getDocUrls() : new ArrayList<>();

        List<ChatMessageDTO> messageDTOs = buildDisplayMessages(messages, sessionDocUrls, sessionImageUrls);

        return ResponseVO.ok(messageDTOs);
    }

    private List<ChatMessageDTO> buildDisplayMessages(List<ChatMessage> messages,
                                                      List<String> sessionDocUrls,
                                                      List<String> sessionImageUrls) {
        List<ChatMessageDTO> result = new ArrayList<>();
        ChatMessageDTO pendingAssistant = null;

        for (ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage userMsg) {
                if (pendingAssistant != null) {
                    result.add(pendingAssistant);
                    pendingAssistant = null;
                }
                result.add(toUserDto(userMsg, sessionDocUrls, sessionImageUrls));
                continue;
            }

            if (msg instanceof dev.langchain4j.data.message.AiMessage aiMsg) {
                if (aiMsg.hasToolExecutionRequests()) {
                    continue;
                }
                String content = aiMsg.text();
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                pendingAssistant = new ChatMessageDTO(
                        "assistant",
                        new ArrayList<>(),
                        new ArrayList<>(),
                        content,
                        System.currentTimeMillis()
                );
            }
        }

        if (pendingAssistant != null) {
            result.add(pendingAssistant);
        }
        return result;
    }

    private ChatMessageDTO toUserDto(dev.langchain4j.data.message.UserMessage userMsg,
                                     List<String> sessionDocUrls,
                                     List<String> sessionImageUrls) {
        StringBuilder textBuilder = new StringBuilder();
        List<String> msgImageUrls = new ArrayList<>();

        for (dev.langchain4j.data.message.Content c : userMsg.contents()) {
            if (c instanceof dev.langchain4j.data.message.TextContent tc) {
                textBuilder.append(tc.text());
            } else if (c instanceof dev.langchain4j.data.message.ImageContent ic) {
                if (ic.image() != null && ic.image().url() != null) {
                    String urlStr = ic.image().url().toString();
                    if (!urlStr.isEmpty()) {
                        msgImageUrls.add(urlStr);
                    }
                }
            }
        }

        List<String> imagUrls = msgImageUrls.isEmpty() ? new ArrayList<>(sessionImageUrls) : msgImageUrls;
        List<String> docUrls = new ArrayList<>(sessionDocUrls);
        return new ChatMessageDTO("user", docUrls, imagUrls, textBuilder.toString(), System.currentTimeMillis());
    }

    /**
     * 获取会话标题
     */
    @GetMapping("/title")
    public ResponseVO<String> getSessionTitle(@RequestParam String memoryId) {
        String userId = tools.getCurrentUserId();
        log.info("=== 获取会话标题，userId: {}, memoryId: {}", userId, memoryId);
        return ResponseVO.ok(chatSessionService.getSessionTitle(memoryId, userId));
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/title")
    public ResponseVO<Void> updateSessionTitle(@RequestParam String memoryId,
                                               @RequestParam String title) {
        String userId = tools.getCurrentUserId();
        log.info("=== 更新会话标题，userId: {}, memoryId: {}, title: {}", userId, memoryId, title);

        // 验证用户权限
        if (chatSessionService.findByMemoryIdAndUserId(memoryId, userId) == null) {
            log.warn("=== 会话不存在或无权操作，memoryId: {}, userId: {}", memoryId, userId);
            return ResponseVO.fail(404, "会话不存在或无权操作");
        }

        chatSessionService.updateSessionTitle(memoryId, userId, title);
        return ResponseVO.ok(null);
    }

    /**
     * 删除指定会话
     */
    @DeleteMapping("/session")
    public ResponseVO<Void> deleteSession(@RequestParam String memoryId) {
        String userId = tools.getCurrentUserId();
        log.info("=== 删除会话，userId: {}, memoryId: {}", userId, memoryId);

        // 验证用户权限
        if (chatSessionService.findByMemoryIdAndUserId(memoryId, userId) == null) {
            log.warn("=== 会话不存在或无权操作，memoryId: {}, userId: {}", memoryId, userId);
            return ResponseVO.fail(404, "会话不存在或无权操作");
        }

        chatSessionService.deleteSession(memoryId, userId);
        return ResponseVO.ok(null);
    }

}
