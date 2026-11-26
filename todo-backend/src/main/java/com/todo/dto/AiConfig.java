package com.todo.dto;

/** 单次 AI 请求使用的接口与模型配置；空值由后端默认值补齐 */
public record AiConfig(String baseUrl, String apiKey, String chatModel, String embedModel) {}
