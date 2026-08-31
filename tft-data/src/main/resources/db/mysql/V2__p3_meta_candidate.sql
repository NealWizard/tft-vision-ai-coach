-- P3 Meta Snapshot and Candidate Set (`P3-META-Snapshot-001`, `P3-DECISION-Candidate-001`)

CREATE TABLE IF NOT EXISTS meta_snapshot (
    id              VARCHAR(128)  NOT NULL PRIMARY KEY,
    patch           VARCHAR(64)   NOT NULL,
    region          VARCHAR(64)   NOT NULL,
    time_window     VARCHAR(32)   NOT NULL,
    captured_at     TIMESTAMP(3)  NOT NULL,
    source_id       VARCHAR(64)   NOT NULL,
    snapshot_json   JSON          NOT NULL
);

CREATE INDEX idx_meta_snapshot_query ON meta_snapshot (patch, region, time_window, captured_at);

CREATE TABLE IF NOT EXISTS candidate_set (
    id              VARCHAR(128)  NOT NULL PRIMARY KEY,
    patch           VARCHAR(64)   NOT NULL,
    decision_type   VARCHAR(32)   NOT NULL,
    fingerprint     VARCHAR(128)  NOT NULL,
    created_at      TIMESTAMP(3)  NOT NULL,
    candidate_json  JSON          NOT NULL
);

CREATE INDEX idx_candidate_set_patch ON candidate_set (patch, created_at);
