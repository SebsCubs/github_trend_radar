package com.trendradar.processor.repository;

import com.trendradar.processor.model.RepositoryMetadata;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RepositoryMetadataRepository extends CrudRepository<RepositoryMetadata, Long> {

    Optional<RepositoryMetadata> findByRepoName(String repoName);

    boolean existsByRepoName(String repoName);
}
