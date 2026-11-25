package com.todo.controller;

import com.todo.auth.CurrentUser;
import com.todo.entity.PomodoroSession;
import com.todo.repository.PomodoroSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pomodoros")
public class PomodoroController {
    private final PomodoroSessionRepository repository;

    public PomodoroController(PomodoroSessionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PomodoroSession> list() {
        return repository.findByUserIdOrderByEndedAtDesc(CurrentUser.get());
    }
}
