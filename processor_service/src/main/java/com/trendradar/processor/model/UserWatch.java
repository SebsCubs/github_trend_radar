package com.trendradar.processor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("user_watches")
public record UserWatch(
    @Id Long id,
    String username,
    String repoName,
    Instant watchedAt
) {
    public static UserWatch create(String username, String repoName) {
        return new UserWatch(null, username, repoName, Instant.now());
    }
}
