package com.todo.repository;

import com.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserIdOrderBySortOrderAscCreatedAtDesc(Long userId);
    List<Todo> findByUserIdAndCompletedOrderBySortOrderAscCreatedAtDesc(Long userId, boolean completed);
}
