package com.project.platform.agents.config.spark;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞星火 Streaming ChatModel 适配器
 */
@Slf4j
public class SparkStreamingChatModel implements StreamingChatModel {

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final String domain;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public SparkStreamingChatModel(String appId, String apiKey, String apiSecret, String domain) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.domain = domain;
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        try {
            String jsonBody = buildRequestBody(chatRequest.messages());
            String authUrl = generateAuthUrl();
            sendWebSocketStreamRequest(authUrl, jsonBody, handler);
        } catch (Exception e) {
            log.error("讯飞星火API调用失败", e);
            handler.onError(e);
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

        Map<String, Object> header = new HashMap<>();
        header.put("app_id", appId);

        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> chat = new HashMap<>();
        chat.put("domain", domain);
        chat.put("temperature", 0.7);
        chat.put("max_tokens", 2048);
        parameter.put("chat", chat);

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("text", messageList);
        payload.put("message", messagePayload);

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
     * 发送WebSocket流式请求
     */
    private void sendWebSocketStreamRequest(String authUrl, String jsonBody,
                                           StreamingChatResponseHandler handler) {
        Request request = new Request.Builder()
                .url(authUrl)
                .build();

        StringBuilder fullResponse = new StringBuilder();

        HTTP_CLIENT.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.debug("WebSocket连接已建立");
                webSocket.send(jsonBody);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    com.alibaba.fastjson2.JSONObject jsonObject = com.alibaba.fastjson2.JSON.parseObject(text);

                    com.alibaba.fastjson2.JSONObject header = jsonObject.getJSONObject("header");
                    if (header != null && header.getInteger("code") != 0) {
                        String errorMsg = String.format("讯飞API返回错误: code=%d, message=%s",
                                header.getInteger("code"), header.getString("message"));
                        log.error(errorMsg);
                        handler.onError(new RuntimeException(errorMsg));
                        return;
                    }

                    com.alibaba.fastjson2.JSONObject payload = jsonObject.getJSONObject("payload");
                    if (payload != null) {
                        com.alibaba.fastjson2.JSONObject choices = payload.getJSONObject("choices");
                        if (choices != null) {
                            com.alibaba.fastjson2.JSONArray textArray = choices.getJSONArray("text");
                            if (textArray != null && !textArray.isEmpty()) {
                                String content = textArray.getJSONObject(0).getString("content");
                                if (content != null) {
                                    handler.onPartialResponse(content);
                                }
                            }
                        }

                        Integer status = choices.getInteger("status");
                        if (status != null && status == 2) {
                            handler.onCompleteResponse(ChatResponse.builder()
                                    .aiMessage(AiMessage.from(fullResponse.toString()))
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.error("解析响应失败", e);
                    handler.onError(e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket连接失败", t);
                handler.onError(t);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.debug("WebSocket连接已关闭: code={}, reason={}", code, reason);
            }
        });
    }
}
