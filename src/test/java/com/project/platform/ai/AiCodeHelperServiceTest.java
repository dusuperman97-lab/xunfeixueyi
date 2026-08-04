package com.project.platform.ai;

import com.project.platform.agents.ai.AiCodeHelperService;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AiCodeHelperServiceTest {
    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void contextLoads() {
    }

    @Test
    void chat(){
//
        System.out.println(aiCodeHelperService.chat("1","我该如何规划我的学习路线"));

    }
    @Test
    void chat1(){
        aiCodeHelperService.streamingChat("2","我要如何规划学习才能进入大厂")
                .doOnNext(System.out::print) // 每接收到一段文本就打印
                .blockLast(); // 阻塞当前线程直到流结束
    }

    @Test
    void chatWithMessage(){
        System.out.println(aiCodeHelperService.streamingChat("1232","\"请为以下章节生成详细的教学内容，以Markdown格式返回。\\n\\n\" +\n" +
                "            \"章节：  \"第1章 Java基础\"\n\\n\\n\" +\n" +
                "            \"小节列表： [\"变量与数据类型\", \"流程控制\"]\\n\\n\" +\n" +
                "            \"要求：\\n\" +\n" +
                "            \"1. 为每个小节生成详细的讲解内容\\n\" +\n" +
                "            \"2. 包含概念解释、代码示例、实践练习\\n\" +\n" +
                "            \"3. 使用Markdown格式，合理使用标题、代码块、列表等\\n\" +\n" +
                "            \"4. 内容要深入浅出，适合初学者理解\\n\" +\n" +
                "            \"5. 每个小节之间用 --- 分隔\"").doOnNext(System.out::print) // 每接收到一段文本就打印
                .blockLast()); // 阻塞当前线程直到流结束);
    }




}