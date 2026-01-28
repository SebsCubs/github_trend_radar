package com.trendradar.ingestor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration class to validate security settings at startup.
 * In production, tokens should be loaded from secure secret management systems
 * (e.g., AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets).
 */
@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${github.token:}")
    private String githubToken;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfiguration() {
        if (githubToken == null || githubToken.isBlank()) {
            logger.error("CRITICAL: GitHub token is not configured. Set GITHUB_TOKEN environment variable.");
            throw new IllegalStateException("GitHub token must be configured. Set GITHUB_TOKEN environment variable.");
        }
        
        // Log token presence (but not the actual token value)
        logger.info("GitHub token is configured (length: {} characters)", githubToken.length());
        
        // Warn if token looks like it might be a placeholder
        if (githubToken.startsWith("github_pat_") && githubToken.length() < 50) {
            logger.warn("GitHub token appears to be invalid or placeholder. Please verify token is correct.");
        }
    }
}