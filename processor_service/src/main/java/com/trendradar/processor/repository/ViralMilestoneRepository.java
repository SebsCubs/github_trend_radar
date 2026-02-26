package com.trendradar.processor.repository;

import com.trendradar.processor.model.ViralMilestone;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ViralMilestoneRepository extends CrudRepository<ViralMilestone, Long> {

    List<ViralMilestone> findByRepoName(String repoName);

    boolean existsByRepoNameAndScoreThreshold(String repoName, double scoreThreshold);
}
