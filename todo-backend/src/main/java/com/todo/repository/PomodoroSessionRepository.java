package com.todo.repository;

import com.todo.entity.PomodoroSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, Long> {
    List<PomodoroSession> findByUserIdOrderByEndedAtDesc(Long userId);
}
