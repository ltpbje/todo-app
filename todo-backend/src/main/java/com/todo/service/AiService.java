package com.todo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.todo.dto.AiConfig;
import com.todo.repository.TodoRepository;
import com.todo.entity.Todo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 面向所有 OpenAI 兼容接口的轻量客户端（DeepSeek、通义/DashScope、Moonshot、
 * SiliconFlow、OpenAI 本身……）。基于 JDK 自带的 HttpClient，无需额外依赖；
 * SSE 流式响应按行消费。
 */
@Service
public class AiService {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;
    private final TodoRepository todoRepository;
    private final String baseUrl;
    private final String apiKey;
    private final String chatModel;
    private final String embedModel;

    public AiService(@Value("${ai.base-url}") String baseUrl,
                     @Value("${ai.api-key}") String apiKey,
                     @Value("${ai.chat-model}") String chatModel,
                     @Value("${ai.embed-model}") String embedModel,
                     ObjectMapper mapper, TodoRepository todoRepository) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embedModel = embedModel;
        this.mapper = mapper;
        this.todoRepository = todoRepository;
    }

    public AiConfig config(Map<String, String> overrides) {
        if (overrides == null) return new AiConfig(baseUrl, apiKey, chatModel, embedModel);
        String b = pick(overrides.get("baseUrl"), baseUrl);
        return new AiConfig(
                b.endsWith("/") ? b.substring(0, b.length() - 1) : b,
                pick(overrides.get("apiKey"), apiKey),
                pick(overrides.get("chatModel"), chatModel),
                pick(overrides.get("embedModel"), embedModel));
    }

    private static String pick(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public boolean enabled() { return enabled(config(null)); }
    public boolean enabled(AiConfig c) { return c.apiKey() != null && !c.apiKey().isBlank(); }

    /** 仅在配置了 embedding 模型时才启用语义能力 */
    public boolean semanticEnabled() { return semanticEnabled(config(null)); }
    public boolean semanticEnabled(AiConfig c) { return enabled(c) && c.embedModel() != null && !c.embedModel().isBlank(); }

    /** 把对话补全的增量片段流式推给消费者（底层是 SSE 的 "data:" 行） */
    public void chatStream(String system, String user, Consumer<String> onDelta) {
        chatStream(config(null), system, user, onDelta);
    }

    public void chatStream(AiConfig c, String system, String user, Consumer<String> onDelta) {
        ObjectNode body = baseBody(c.chatModel());
        body.put("stream", true);
        addMessages(body, system, user);

        HttpRequest req = request(c, "/chat/completions", body).timeout(Duration.ofSeconds(120)).build();
        try {
            HttpResponse<java.util.stream.Stream<String>> resp =
                    http.send(req, HttpResponse.BodyHandlers.ofLines());
            if (resp.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务错误（HTTP " + resp.statusCode() + "）");
            }
            resp.body().forEach(line -> {
                if (!line.startsWith("data:")) return;
                String data = line.substring(5).trim();
                if (data.isEmpty() || data.equals("[DONE]")) return;
                try {
                    JsonNode content = mapper.readTree(data)
                            .path("choices").path(0).path("delta").path("content");
                    if (content.isTextual()) onDelta.accept(content.asText());
                } catch (Exception ignored) {
                    // 片段格式异常——跳过它，流继续
                }
            });
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务连接失败");
        }
    }

    /** 非流式对话，要求返回一个 JSON 对象（JSON 模式） */
    public String chatJson(String system, String user) {
        return chatJson(config(null), system, user);
    }

    public String chatJson(AiConfig c, String system, String user) {
        ObjectNode body = baseBody(c.chatModel());
        body.set("response_format", mapper.createObjectNode().put("type", "json_object"));
        addMessages(body, system, user);

        HttpRequest req = request(c, "/chat/completions", body).timeout(Duration.ofSeconds(60)).build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务错误（HTTP " + resp.statusCode() + "）");
            }
            return mapper.readTree(resp.body()).path("choices").path(0).path("message").path("content").asText();
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务连接失败");
        }
    }

    /** 文本 → 向量，调用 /embeddings 接口 */
    public double[] embed(String text) {
        return embed(config(null), text);
    }

    public double[] embed(AiConfig c, String text) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", c.embedModel());
        body.putArray("input").add(text);

        HttpRequest req = request(c, "/embeddings", body).timeout(Duration.ofSeconds(30)).build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding 服务错误（HTTP " + resp.statusCode() + "）");
            }
            JsonNode arr = mapper.readTree(resp.body()).path("data").path(0).path("embedding");
            double[] vec = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = arr.get(i).asDouble();
            return vec;
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding 服务连接失败");
        }
    }

    /** 尽力而为：把待办标题的向量以 JSON 数组字符串存入 */
    public void embedTodo(Todo todo) {
        if (!semanticEnabled()) return;
        try {
            todo.setEmbedding(mapper.writeValueAsString(embed(todo.getTitle())));
        } catch (Exception e) {
            // ponytail: 向量计算是尽力而为——调用失败只意味着这条待办
            // 回退到关键字搜索；下次 /ai/search 时会惰性回填
        }
    }

    /** 读取已存的向量，若没有则即时计算并持久化（惰性回填） */
    public double[] ensureEmbedding(Todo todo) {
        return ensureEmbedding(todo, config(null));
    }

    public double[] ensureEmbedding(Todo todo, AiConfig c) {
        if (todo.getEmbedding() != null) {
            try { return mapper.readValue(todo.getEmbedding(), double[].class); }
            catch (Exception ignored) { /* 数据陈旧或损坏——在下方重新计算 */ }
        }
        if (!semanticEnabled(c)) return null;
        try {
            double[] vec = embed(c, todo.getTitle());
            todo.setEmbedding(mapper.writeValueAsString(vec));
            todoRepository.save(todo);
            return vec;
        } catch (Exception e) {
            return null;
        }
    }

    /** 余弦相似度；任一向量缺失时返回 0 */
    public static double similarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private ObjectNode baseBody(String model) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        return body;
    }

    private void addMessages(ObjectNode body, String system, String user) {
        ArrayNode msgs = body.putArray("messages");
        msgs.addObject().put("role", "system").put("content", system);
        msgs.addObject().put("role", "user").put("content", user);
    }

    private HttpRequest.Builder request(AiConfig c, String path, ObjectNode body) {
        try {
            return HttpRequest.newBuilder(URI.create(c.baseUrl() + path))
                    .header("Authorization", "Bearer " + c.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "请求构造失败");
        }
    }
}
