package com.project.platform.agents.config.spark;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞星火 ChatModel 适配器
 */
@Slf4j
public class SparkChatModel implements ChatModel {

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final String domain;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public SparkChatModel(String appId, String apiKey, String apiSecret, String domain) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.domain = domain;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        try {
            String jsonBody = buildRequestBody(chatRequest.messages());
            String authUrl = generateAuthUrl();
            String responseText = sendWebSocketRequest(authUrl, jsonBody);
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(responseText))
                    .build();
        } catch (Exception e) {
            log.error("讯飞星火API调用失败", e);
            throw new RuntimeException("讯飞星火API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(List<dev.langchain4j.data.message.ChatMessage> messages) {
        List<Map<String, String>> messageList = new ArrayList<>();
        
        for (dev.langchain4j.data.message.ChatMessage message : messages) {
            Map<String, String> msg = new HashMap<>();
            
            if (message instanceof dev.langchain4j.data.message.UserMessage userMsg) {
                msg.put("role", "user");
                msg.put("content", userMsg.singleText());
            } else if (message instanceof dev.langchain4j.data.message.AiMessage aiMsg) {
                msg.put("role", "assistant");
                String text = aiMsg.text();
                msg.put("content", text != null ? text : "");
            } else if (message instanceof dev.langchain4j.data.message.SystemMessage sysMsg) {
                msg.put("role", "system");
                String text = sysMsg.text();
                msg.put("content", text != null ? text : "");
            }

            messageList.add(msg);
        }

        // 构建请求头部分，包含应用ID标识
        Map<String, Object> header = new HashMap<>();
        header.put("app_id", appId);

        // 构建参数配置部分，设置对话领域、温度系数和最大token数
        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> chat = new HashMap<>();
        chat.put("domain", domain);
        chat.put("temperature", 0.7);
        chat.put("max_tokens", 2048);
        parameter.put("chat", chat);

        // 构建消息负载部分，封装聊天消息列表
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("text", messageList);
        payload.put("message", messagePayload);

        // 组装完整的请求体结构，包含头部、参数和负载三部分
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("header", header);
        requestBody.put("parameter", parameter);
        requestBody.put("payload", payload);

        return com.alibaba.fastjson2.JSON.toJSONString(requestBody);
    }

    /**
     * 生成鉴权URL
     */
    private String generateAuthUrl() throws Exception {
        String host = "spark-api.xf-yun.com";
        String path = "/v3.5/chat";

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        String preStr = "host: " + host + "\n" +
                "date: " + date + "\n" +
                "GET " + path + " HTTP/1.1";

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = Base64.getEncoder().encodeToString(
                ("api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + sha + "\"")
                        .getBytes(StandardCharsets.UTF_8));

        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + host + path)).newBuilder()
                .addQueryParameter("authorization", authorization)
                .addQueryParameter("date", date)
                .addQueryParameter("host", host)
                .build();

        return httpUrl.toString().replace("https://", "wss://");
    }

    /**
     * 发送WebSocket请求(同步方式)
     */
    private String sendWebSocketRequest(String authUrl, String jsonBody) throws Exception {
        Request request = new Request.Builder()
                .url(authUrl)
                .build();

        StringBuilder responseBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        WebSocket webSocket = HTTP_CLIENT.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.debug("WebSocket连接已建立");
                webSocket.send(jsonBody);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                log.debug("收到消息: {}", text);
                try {
                    com.alibaba.fastjson2.JSONObject jsonObject = com.alibaba.fastjson2.JSON.parseObject(text);

                    com.alibaba.fastjson2.JSONObject header = jsonObject.getJSONObject("header");
                    if (header != null && header.getInteger("code") != 0) {
                        log.error("讯飞API返回错误: code={}, message={}",
                                header.getInteger("code"), header.getString("message"));
                        latch.countDown();
                        return;
                    }

                    com.alibaba.fastjson2.JSONObject payload = jsonObject.getJSONObject("payload");
                    if (payload != null) {
                        com.alibaba.fastjson2.JSONObject choices = payload.getJSONObject("choices");
                        if (choices != null) {
                            com.alibaba.fastjson2.JSONArray textArray = choices.getJSONArray("text");
                            if (textArray != null && !textArray.isEmpty()) {
                                String content = textArray.getJSONObject(0).getString("content");
                                responseBuilder.append(content);
                            }

                            Integer status = choices.getInteger("status");
                            if (status != null && status == 2) {
                                latch.countDown();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("解析响应失败", e);
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket连接失败", t);
                latch.countDown();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.debug("WebSocket连接已关闭: code={}, reason={}", code, reason);
                latch.countDown();
            }
        });

        latch.await(60, TimeUnit.SECONDS);
        webSocket.close(1000, "Done");

        return responseBuilder.toString();
    }
}
