package com.trendradar.processor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("viral_milestones")
public record ViralMilestone(
    @Id Long id,
    String repoName,
    double scoreThreshold,
    Instant reachedAt
) {
    public static ViralMilestone record(String repoName, double threshold) {
        return new ViralMilestone(null, repoName, threshold, Instant.now());
    }
}
