package com.todo.repository;

import com.todo.entity.UserAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAiConfigRepository extends JpaRepository<UserAiConfig, Long> {
    Optional<UserAiConfig> findByUserId(Long userId);
}
