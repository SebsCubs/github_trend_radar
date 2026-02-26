package com.trendradar.processor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("repository_metadata")
public record RepositoryMetadata(
    @Id Long id,
    String repoName,
    String description,
    String primaryLanguage,
    String license,
    String owner,
    Instant firstSeenAt,
    Instant lastUpdatedAt
) {
    public static RepositoryMetadata createNew(String repoName, String owner,
                                                String description, String primaryLanguage,
                                                String license) {
        Instant now = Instant.now();
        return new RepositoryMetadata(null, repoName, description, primaryLanguage, license, owner, now, now);
    }

    public RepositoryMetadata withUpdatedMetadata(String description, String primaryLanguage,
                                                   String license) {
        return new RepositoryMetadata(id, repoName, description, primaryLanguage, license, owner, firstSeenAt, Instant.now());
    }
}
