package com.trendradar.processor.service;

import com.trendradar.common.model.GithubEvent;
import com.trendradar.processor.model.ViralMilestone;
import com.trendradar.processor.repository.TrendingRepository;
import com.trendradar.processor.repository.ViralMilestoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);
    private static final List<Double> VIRAL_THRESHOLDS = List.of(50.0, 100.0, 500.0, 1000.0, 5000.0);

    private final TrendingRepository trendingRepository;
    private final GithubMetadataService metadataService;
    private final ViralMilestoneRepository milestoneRepository;

    public EventProcessor(TrendingRepository trendingRepository,
                          GithubMetadataService metadataService,
                          ViralMilestoneRepository milestoneRepository) {
        this.trendingRepository = trendingRepository;
        this.metadataService = metadataService;
        this.milestoneRepository = milestoneRepository;
    }

    @KafkaListener(topics = "raw-github-events", groupId = "trend-radar-processor-group")
    public void consumeEvent(GithubEvent event) {
        if (event.repo() == null || event.type() == null) return;

        String repoName = event.repo().name();

        // Java 21 Switch Expression
        double score = switch (event.type()) {
            case "WatchEvent"       -> 1.0;
            case "ForkEvent"        -> 3.0;
            case "PullRequestEvent" -> 5.0;
            case "IssuesEvent"      -> 2.0;
            case "PushEvent"        -> 0.5;
            default                 -> 0.0;
        };

        if (score > 0) {
            log.info("Boosting {} by {} points (Event: {})", repoName, score, event.type());

            // 1. Update Redis leaderboard (real-time)
            double previousScore = trendingRepository.getScore(repoName);
            trendingRepository.addScore(repoName, score);
            double newScore = previousScore + score;

            // 2. If first time seeing this repo, fetch metadata from GitHub → PostgreSQL
            metadataService.fetchAndStoreIfNew(repoName);

            // 3. Check if a viral threshold was crossed → PostgreSQL
            checkViralMilestones(repoName, previousScore, newScore);
        }
    }

    private void checkViralMilestones(String repoName, double previousScore, double newScore) {
        for (double threshold : VIRAL_THRESHOLDS) {
            if (previousScore < threshold && newScore >= threshold) {
                if (!milestoneRepository.existsByRepoNameAndScoreThreshold(repoName, threshold)) {
                    ViralMilestone milestone = ViralMilestone.record(repoName, threshold);
                    milestoneRepository.save(milestone);
                    log.info("VIRAL MILESTONE: {} hit {} points!", repoName, threshold);
                }
            }
        }
    }
}