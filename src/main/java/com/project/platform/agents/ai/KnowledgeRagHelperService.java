package com.project.platform.agents.ai;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel"
)
public interface KnowledgeRagHelperService {

    String chat(String question);

    Flux<String> streamChat(String question);
}
