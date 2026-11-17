package com.todo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.auth.CurrentUser;
import com.todo.entity.Todo;
import com.todo.repository.TodoRepository;
import com.todo.service.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService ai;
    private final TodoRepository todoRepository;
    private final ObjectMapper mapper;

    public AiController(AiService ai, TodoRepository todoRepository, ObjectMapper mapper) {
        this.ai = ai;
        this.todoRepository = todoRepository;
        this.mapper = mapper;
    }

    /**
     * 把一个目标拆解成子任务。以 SSE 事件流式输出纯文本片段
     * （每个载荷是一个 JSON 字符串；{"done":true} 表示结束）。
     */
    @PostMapping("/breakdown")
    public SseEmitter breakdown(@RequestBody Map<String, String> body) {
        requireEnabled();
        String title = body.getOrDefault("title", "").trim();
        String description = body.getOrDefault("description", "").trim();
        if (title.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少任务标题");

        String system = "你是任务拆解助手。把用户给出的目标拆解成 3-6 个可执行的子任务。"
                + "只输出子任务本身，每行一个，不要编号，不要任何解释或多余文字。";

        SseEmitter emitter = new SseEmitter(120_000L);
        // ponytail: 这里用一次性工作线程而非线程池——演示项目最多同时有
        // 几个流；若以后复用率提高，再换成有界线程池
        new Thread(() -> {
            try {
                ai.chatStream(system, title + (description.isEmpty() ? "" : "\n" + description), chunk -> {
                    try { emitter.send(mapper.writeValueAsString(chunk)); }
                    catch (Exception ignored) { /* client gone */ }
                });
                emitter.send(mapper.writeValueAsString(Map.of("done", true)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    /** 自然语言 → 结构化任务（JSON 模式） */
    @PostMapping("/parse")
    public Map<String, Object> parse(@RequestBody Map<String, String> body) {
        requireEnabled();
        String text = body.getOrDefault("text", "").trim();
        if (text.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "输入为空");

        String system = "你是待办提取助手。从用户输入中提取每一个待办任务。今天是 " + LocalDate.now() + "。"
                + "只输出一个 JSON 对象，格式："
                + "{\"tasks\":[{\"title\":\"简短的任务名\",\"dueDate\":\"YYYY-MM-DD 或 null\"}]}。"
                + "不要输出任何其它内容。";

        try {
            JsonNode tasks = mapper.readTree(ai.chatJson(system, text)).path("tasks");
            return Map.of("tasks", mapper.convertValue(tasks, List.class));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回的内容无法解析");
        }
    }

    /** 在当前用户的待办中做语义搜索；未启用 AI/embedding 时回退到关键字匹配 */
    @PostMapping("/search")
    public List<Todo> search(@RequestBody Map<String, String> body) {
        String q = body.getOrDefault("q", "").trim();
        List<Todo> todos = todoRepository.findByUserIdOrderByCreatedAtDesc(CurrentUser.get());
        if (q.isEmpty()) return todos;

        if (!ai.semanticEnabled()) {
            String lower = q.toLowerCase();
            return todos.stream()
                    .filter(t -> (t.getTitle() + " " + (t.getDescription() == null ? "" : t.getDescription()))
                            .toLowerCase().contains(lower))
                    .toList();
        }

        double[] qv = ai.embed(q);
        record Scored(Todo todo, double score) {}
        return todos.stream()
                .map(t -> new Scored(t, AiService.similarity(qv, ai.ensureEmbedding(t))))
                .filter(s -> s.score() > 0.25)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(20)
                .map(Scored::todo)
                .toList();
    }

    private void requireEnabled() {
        if (!ai.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 功能未启用：请设置环境变量 AI_API_KEY 后重启后端");
        }
    }
}
