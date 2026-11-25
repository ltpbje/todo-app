package com.todo.service;

import com.todo.auth.CurrentUser;
import com.todo.dto.TodoDTO;
import com.todo.entity.PomodoroSession;
import com.todo.entity.Todo;
import com.todo.repository.PomodoroSessionRepository;
import com.todo.repository.TodoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final PomodoroSessionRepository sessionRepository;
    private final AiService aiService;

    public TodoService(TodoRepository todoRepository, PomodoroSessionRepository sessionRepository, AiService aiService) {
        this.todoRepository = todoRepository;
        this.sessionRepository = sessionRepository;
        this.aiService = aiService;
    }

    public List<Todo> list(Boolean completed) {
        Long userId = CurrentUser.get();
        if (completed == null) {
            return todoRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(userId);
        }
        return todoRepository.findByUserIdAndCompletedOrderBySortOrderAscCreatedAtDesc(userId, completed);
    }

    /** 按前端给出的 id 顺序重排；只处理当前用户的任务 */
    public void reorder(List<Long> ids) {
        Map<Long, Todo> owned = new LinkedHashMap<>();
        for (Todo t : list(null)) owned.put(t.getId(), t);
        int order = 0;
        for (Long id : ids) {
            Todo t = owned.remove(id);
            if (t != null) t.setSortOrder(order++);
        }
        // 本次未出现在列表里的任务仍要有确定顺序
        for (Todo t : owned.values()) t.setSortOrder(order++);
        todoRepository.saveAll(list(null));
    }

    public Todo addPomodoro(Long id) {
        Todo todo = owned(id);
        LocalDateTime now = LocalDateTime.now();
        todo.setPomodoroCount(todo.getPomodoroCount() + 1);
        todo.setLastPomodoroAt(now);

        PomodoroSession session = new PomodoroSession();
        session.setUserId(todo.getUserId());
        session.setTodoId(todo.getId());
        session.setTodoTitle(todo.getTitle());
        session.setDurationSec(25 * 60);
        session.setEndedAt(now);
        sessionRepository.save(session);
        return todoRepository.save(todo);
    }

    private Todo owned(Long id) {
        return todoRepository.findById(id)
                .filter(t -> t.getUserId().equals(CurrentUser.get()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));
    }

    public Todo create(TodoDTO dto) {
        Todo todo = new Todo();
        todo.setUserId(CurrentUser.get());
        apply(todo, dto);
        aiService.embedTodo(todo);
        return todoRepository.save(todo);
    }

    public List<Todo> createBatch(List<TodoDTO> dtos) {
        return dtos.stream().map(this::create).toList();
    }

    public Todo update(Long id, TodoDTO dto) {
        Todo todo = owned(id);
        apply(todo, dto);
        aiService.embedTodo(todo);   // title may have changed
        return todoRepository.save(todo);
    }

    public void delete(Long id) {
        todoRepository.delete(owned(id));
    }

    private void apply(Todo todo, TodoDTO dto) {
        boolean wasCompleted = todo.isCompleted();
        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setCompleted(dto.isCompleted());
        todo.setDueDate(dto.getDueDate());
        todo.setCategory(dto.getCategory());
        todo.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        todo.setRemindAt(dto.getRemindAt());
        todo.setRepeatRule(dto.getRepeatRule() == null || dto.getRepeatRule().isBlank() ? "none" : dto.getRepeatRule());
        todo.setParentId(dto.getParentId());
        if (dto.isCompleted() && !wasCompleted) {
            todo.setCompletedAt(LocalDateTime.now());
            generateNextOccurrence(todo);
        }
        if (!dto.isCompleted()) todo.setCompletedAt(null);
    }

    /** 重复任务完成后生成下一期；已存在未完成的下期则跳过，避免重复点击造成多条 */
    private void generateNextOccurrence(Todo todo) {
        String rule = todo.getRepeatRule();
        int days = "daily".equals(rule) ? 1 : "weekly".equals(rule) ? 7 : 0;
        if (days == 0) return;

        LocalDate nextDue = todo.getDueDate() == null ? LocalDate.now().plusDays(days) : todo.getDueDate().plusDays(days);
        boolean exists = list(null).stream().anyMatch(t -> !t.isCompleted()
                && t.getParentId() != null && t.getParentId().equals(todo.getId())
                && Objects.equals(t.getTitle(), todo.getTitle()));
        if (exists) return;

        Todo next = new Todo();
        next.setUserId(todo.getUserId());
        next.setTitle(todo.getTitle());
        next.setDescription(todo.getDescription());
        next.setDueDate(nextDue);
        next.setPriority(todo.getPriority());
        next.setCategory(todo.getCategory());
        next.setRepeatRule(rule);
        next.setParentId(todo.getId());
        next.setRemindAt(todo.getRemindAt() == null ? null : todo.getRemindAt().plusDays(days));
        next.setSortOrder(todo.getSortOrder());
        todoRepository.save(next);
    }
}
