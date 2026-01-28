package com.trendradar.ingestor;

import com.trendradar.common.model.GithubEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class GithubIngestorService {

    private static final Logger logger = LoggerFactory.getLogger(GithubIngestorService.class);
    
    private final RestClient restClient;
    private final KafkaTemplate<String, GithubEvent> kafkaTemplate;
    private final String kafkaTopic;

    public GithubIngestorService(RestClient.Builder builder, 
                                 KafkaTemplate<String, GithubEvent> kafkaTemplate,
                                 @Value("${github.token}") String token,
                                 @Value("${github.api-url:https://api.github.com}") String apiBaseUrl,
                                 @Value("${kafka.topic.raw-events:raw-github-events}") String kafkaTopic) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("GitHub token must be provided");
        }
        
        this.kafkaTopic = kafkaTopic;
        // Configure RestClient to use a Virtual Thread executor for its underlying HTTP client
        this.restClient = builder
                .baseUrl(apiBaseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .requestFactory(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder()
                        .executor(Executors.newVirtualThreadPerTaskExecutor())
                        .build()
                ))
                .build();
        this.kafkaTemplate = kafkaTemplate;
        
        logger.info("GithubIngestorService initialized with API base URL: {}", apiBaseUrl);
    }

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void fetchAndStreamEvents() {
        try {
            logger.debug("Fetching GitHub events from API");
            
            List<GithubEvent> events = restClient.get()
                    .uri("/events")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GithubEvent>>() {});

            if (events == null || events.isEmpty()) {
                logger.debug("No events received from GitHub API");
                return;
            }

            logger.info("Received {} events from GitHub API", events.size());
            
            int highValueCount = 0;
            int processedCount = 0;
            int errorCount = 0;

            for (GithubEvent event : events) {
                try {
                    // Null safety checks
                    if (event == null) {
                        logger.warn("Received null event, skipping");
                        continue;
                    }
                    
                    if (!event.isHighValue()) {
                        continue;
                    }
                    
                    highValueCount++;
                    
                    // Null safety for repo
                    if (event.repo() == null) {
                        logger.warn("Event {} has null repo, skipping", event.id());
                        continue;
                    }
                    
                    if (event.repo().name() == null || event.repo().name().isBlank()) {
                        logger.warn("Event {} has null or blank repo name, skipping", event.id());
                        continue;
                    }

                    // Handle Kafka send with proper error handling
                    String repoName = event.repo().name();
                    CompletableFuture<SendResult<String, GithubEvent>> future = 
                        kafkaTemplate.send(kafkaTopic, repoName, event);
                    
                    future.whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to send event {} for repo {} to Kafka topic {}", 
                                event.id(), repoName, kafkaTopic, ex);
                        } else {
                            logger.debug("Successfully sent event {} for repo {} to Kafka", 
                                event.id(), repoName);
                        }
                    });
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error processing event {}", event != null ? event.id() : "null", e);
                }
            }
            
            logger.info("Event processing complete - High value: {}, Processed: {}, Errors: {}", 
                highValueCount, processedCount, errorCount);
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP client error while fetching GitHub events: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            logger.error("HTTP server error while fetching GitHub events: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            logger.error("Rest client error while fetching GitHub events", e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching and streaming GitHub events", e);
        }
    }
}