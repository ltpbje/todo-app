package com.todo.service;

import com.todo.auth.CurrentUser;
import com.todo.dto.TodoDTO;
import com.todo.entity.Todo;
import com.todo.repository.TodoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public List<Todo> list(Boolean completed) {
        Long userId = CurrentUser.get();
        if (completed == null) {
            return todoRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return todoRepository.findByUserIdAndCompletedOrderByCreatedAtDesc(userId, completed);
    }

    public Todo create(TodoDTO dto) {
        Todo todo = new Todo();
        todo.setUserId(CurrentUser.get());
        apply(todo, dto);
        return todoRepository.save(todo);
    }

    public Todo update(Long id, TodoDTO dto) {
        Todo todo = todoRepository.findById(id)
                .filter(t -> t.getUserId().equals(CurrentUser.get()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));
        apply(todo, dto);
        return todoRepository.save(todo);
    }

    public void delete(Long id) {
        Todo todo = todoRepository.findById(id)
                .filter(t -> t.getUserId().equals(CurrentUser.get()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));
        todoRepository.delete(todo);
    }

    private void apply(Todo todo, TodoDTO dto) {
        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setCompleted(dto.isCompleted());
    }
}
