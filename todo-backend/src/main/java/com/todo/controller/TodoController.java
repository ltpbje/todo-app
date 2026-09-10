package com.todo.controller;

import com.todo.dto.TodoDTO;
import com.todo.entity.Todo;
import com.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<Todo> list(@RequestParam(required = false) Boolean completed) {
        return todoService.list(completed);
    }

    @PostMapping
    public Todo create(@Valid @RequestBody TodoDTO dto) {
        return todoService.create(dto);
    }

    @PutMapping("/{id}")
    public Todo update(@PathVariable Long id, @Valid @RequestBody TodoDTO dto) {
        return todoService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
