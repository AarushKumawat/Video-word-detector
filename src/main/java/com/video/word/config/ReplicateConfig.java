package com.video.word.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "replicate")
public class ReplicateConfig {
    private String apiKey;
    private String extensionId;

    @PostConstruct
    public void init() {
        System.out.println("Replicate API Key: " + apiKey);
        System.out.println("Replicate Extension ID: " + extensionId);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public void setExtensionId(String extensionId) {
        this.extensionId = extensionId;
    }
}
