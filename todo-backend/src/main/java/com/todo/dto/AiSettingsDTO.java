package com.todo.dto;

public class AiSettingsDTO {
    private String baseUrl;
    private String apiKey;
    private String chatModel;
    private String embedModel;
    private boolean apiKeySet;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public String getEmbedModel() { return embedModel; }
    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }
    public boolean isApiKeySet() { return apiKeySet; }
    public void setApiKeySet(boolean apiKeySet) { this.apiKeySet = apiKeySet; }
}
