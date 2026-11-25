package com.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TodoDTO {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 20000)
    private String description;

    private boolean completed;

    /** ISO 日期 yyyy-MM-dd，null 表示不安排到某天 */
    private LocalDate dueDate;

    private String category;
    private Integer priority;
    private LocalDateTime remindAt;
    private String repeatRule;
    private Long parentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getRemindAt() { return remindAt; }
    public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
    public String getRepeatRule() { return repeatRule; }
    public void setRepeatRule(String repeatRule) { this.repeatRule = repeatRule; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
