package com.trendradar.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubEvent(
    String id,
    String type,
    Actor actor,
    Repo repo,
    @JsonProperty("created_at") Instant createdAt
) {
    
    public boolean isHighValue() {
        return "WatchEvent".equals(type) 
            || "ForkEvent".equals(type)
            || "PushEvent".equals(type);  // Added for testing - very common event
    }
}