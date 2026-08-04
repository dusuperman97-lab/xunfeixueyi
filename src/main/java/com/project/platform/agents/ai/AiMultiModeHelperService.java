package com.project.platform.agents.ai;

import com.project.platform.agents.config.mongdb.PersistentChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AiMultiModeHelperService {

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    @Resource
    private PersistentChatMemoryStore chatMemoryStore; // 直接操作Memory存储

    /**
     * 多模态流式聊天 - 绕开AiService代理，手动管理Memory
     * 图片只在本轮发送，不存入Memory
     */
    public Flux<String> streamingChatWithMultimodal(String memoryId,
                                                    UserMessage userMessage,
                                                    String textForMemory) {
        // 1. 读取历史纯文本消息
        List<ChatMessage> history = chatMemoryStore.getMessages(memoryId);

        // 2. 读取 system prompt
        SystemMessage systemMessage = SystemMessage.from(loadSystemPrompt());

        // 3. 拼装本轮消息：system + 历史 + 当前含图消息
        List<ChatMessage> messagesForModel = new ArrayList<>();
        messagesForModel.add(systemMessage);
        messagesForModel.addAll(history);
        messagesForModel.add(userMessage); // 含Base64，只发模型不存库

        // 4. 流式调用
        return Flux.create(sink -> {
            StringBuilder fullResponse = new StringBuilder();

            qwenStreamingChatModel.chat(messagesForModel,
                    new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String token) {
                            sink.next(token);
                            fullResponse.append(token);
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            sink.complete();
                            // 5. 存Memory：纯文本摘要 + AI回复
                            try {
                                List<ChatMessage> toSave = new ArrayList<>(history);
                                toSave.add(UserMessage.from(textForMemory)); // 纯文本，不含图片
                                toSave.add(AiMessage.from(fullResponse.toString()));
                                chatMemoryStore.updateMessages(memoryId, toSave);
                                log.info("=== 多模态Memory更新完成, memoryId={}", memoryId);
                            } catch (Exception e) {
                                log.error("=== 更新Memory失败", e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("=== 流式响应错误", error);
                            sink.error(error);
                        }
                    });
        });
    }

    // 加载system prompt
    private String loadSystemPrompt() {
        try {
            return new String(getClass().getClassLoader()
                    .getResourceAsStream("system-prompt.txt").readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("=== 加载system-prompt.txt失败，使用空prompt");
            return "";
        }
    }
}
