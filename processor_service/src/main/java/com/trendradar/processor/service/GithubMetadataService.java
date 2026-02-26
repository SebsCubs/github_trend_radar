package com.trendradar.processor.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trendradar.processor.model.RepositoryMetadata;
import com.trendradar.processor.repository.RepositoryMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.Executors;

@Service
public class GithubMetadataService {

    private static final Logger log = LoggerFactory.getLogger(GithubMetadataService.class);

    private final RestClient restClient;
    private final RepositoryMetadataRepository metadataRepository;

    public GithubMetadataService(RestClient.Builder builder,
                                  RepositoryMetadataRepository metadataRepository,
                                  @Value("${github.token:}") String token,
                                  @Value("${github.api-url:https://api.github.com}") String apiBaseUrl) {
        this.metadataRepository = metadataRepository;

        var clientBuilder = builder
                .baseUrl(apiBaseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder()
                        .executor(Executors.newVirtualThreadPerTaskExecutor())
                        .build()
                ));

        if (token != null && !token.isBlank()) {
            clientBuilder.defaultHeader("Authorization", "Bearer " + token);
        }

        this.restClient = clientBuilder.build();
    }

    /**
     * Fetches repo metadata from GitHub and saves it to PostgreSQL.
     * Uses a virtual thread so this blocking I/O doesn't pin platform threads.
     */
    public void fetchAndStoreIfNew(String repoFullName) {
        Thread.startVirtualThread(() -> {
            try {
                if (metadataRepository.existsByRepoName(repoFullName)) {
                    return;
                }

                log.info("New repository detected: {}. Fetching metadata from GitHub.", repoFullName);

                String[] parts = repoFullName.split("/", 2);
                if (parts.length != 2) {
                    log.warn("Invalid repo name format (expected owner/repo): {}", repoFullName);
                    return;
                }

                GithubRepoResponse response = restClient.get()
                        .uri("/repos/{owner}/{repo}", parts[0], parts[1])
                        .retrieve()
                        .body(GithubRepoResponse.class);

                if (response == null) {
                    log.warn("GitHub API returned null for repo: {}", repoFullName);
                    return;
                }

                String owner = (response.owner() != null) ? response.owner().login() : extractOwner(repoFullName);
                String license = (response.license() != null) ? response.license().spdxId() : null;

                RepositoryMetadata metadata = RepositoryMetadata.createNew(
                    repoFullName,
                    owner,
                    response.description(),
                    response.language(),
                    license
                );

                metadataRepository.save(metadata);
                log.info("Stored metadata for repository: {} (Language: {}, License: {})",
                    repoFullName, response.language(), license);

            } catch (RestClientException e) {
                log.warn("Failed to fetch metadata for {}: {}", repoFullName, e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error storing metadata for {}", repoFullName, e);
            }
        });
    }

    public Optional<RepositoryMetadata> getMetadata(String repoName) {
        return metadataRepository.findByRepoName(repoName);
    }

    private static String extractOwner(String repoFullName) {
        int slash = repoFullName.indexOf('/');
        return (slash > 0) ? repoFullName.substring(0, slash) : "unknown";
    }

    // DTOs for the GitHub API response
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GithubRepoResponse(
        String description,
        String language,
        GithubOwner owner,
        GithubLicense license
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GithubOwner(String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GithubLicense(@JsonProperty("spdx_id") String spdxId) {}
}
