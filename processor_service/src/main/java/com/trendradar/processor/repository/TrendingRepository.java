package com.trendradar.processor.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TrendingRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String LEADERBOARD_KEY = "trending:repos";

    public TrendingRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addScore(String repoName, double score) {
        redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, repoName, score);
    }

    public double getScore(String repoName) {
        Double score = redisTemplate.opsForZSet().score(LEADERBOARD_KEY, repoName);
        return (score != null) ? score : 0.0;
    }
}