-- Repository Metadata: the "profile" of each repo
CREATE TABLE IF NOT EXISTS repository_metadata (
    id              BIGSERIAL PRIMARY KEY,
    repo_name       VARCHAR(255) UNIQUE NOT NULL,
    description     TEXT,
    primary_language VARCHAR(100),
    license         VARCHAR(100),
    owner           VARCHAR(255) NOT NULL,
    first_seen_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Viral Milestones: historical log of when a repo hit a score threshold
CREATE TABLE IF NOT EXISTS viral_milestones (
    id              BIGSERIAL PRIMARY KEY,
    repo_name       VARCHAR(255) NOT NULL,
    score_threshold DOUBLE PRECISION NOT NULL,
    reached_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(repo_name, score_threshold)
);

-- User Watches: maps users to repositories they follow
CREATE TABLE IF NOT EXISTS user_watches (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    repo_name   VARCHAR(255) NOT NULL,
    watched_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(username, repo_name)
);

CREATE INDEX IF NOT EXISTS idx_repo_metadata_name ON repository_metadata(repo_name);
CREATE INDEX IF NOT EXISTS idx_viral_milestones_repo ON viral_milestones(repo_name);
CREATE INDEX IF NOT EXISTS idx_user_watches_username ON user_watches(username);
CREATE INDEX IF NOT EXISTS idx_user_watches_repo ON user_watches(repo_name);
