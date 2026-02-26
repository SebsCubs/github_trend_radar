package com.trendradar.processor.repository;

import com.trendradar.processor.model.UserWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserWatchRepository extends CrudRepository<UserWatch, Long> {

    List<UserWatch> findByUsername(String username);

    List<UserWatch> findByRepoName(String repoName);

    boolean existsByUsernameAndRepoName(String username, String repoName);
}
