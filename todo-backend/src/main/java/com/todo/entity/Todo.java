package com.todo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 标题向量的 JSON 数组；为 null 表示未计算（回退到关键字搜索） */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 越小越重要；拖拽排序时按可见顺序重新编号 */
    @Column(name = "sort_order", nullable = false, columnDefinition = "int default 0")
    private int sortOrder = 0;

    @Column(name = "pomodoro_count", nullable = false, columnDefinition = "int default 0")
    private int pomodoroCount = 0;

    @Column(name = "last_pomodoro_at")
    private LocalDateTime lastPomodoroAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 清单名：null 表示默认清单 */
    @Column(length = 40)
    private String category;

    /** 0 普通 / 1 重要 / 2 紧急 */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int priority = 0;

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    /** none / daily / weekly */
    @Column(name = "repeat_rule", length = 16)
    private String repeatRule = "none";

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public int getPomodoroCount() { return pomodoroCount; }
    public void setPomodoroCount(int pomodoroCount) { this.pomodoroCount = pomodoroCount; }
    public LocalDateTime getLastPomodoroAt() { return lastPomodoroAt; }
    public void setLastPomodoroAt(LocalDateTime lastPomodoroAt) { this.lastPomodoroAt = lastPomodoroAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getRemindAt() { return remindAt; }
    public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
    public String getRepeatRule() { return repeatRule; }
    public void setRepeatRule(String repeatRule) { this.repeatRule = repeatRule; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
