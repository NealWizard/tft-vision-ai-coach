-- P1 store tables (`P1-DATA-Evidence-001`, `P1-DATA-Conflict-001`, `P1-DATA-Normalize-001`, `P1-DATA-Patch-001`)

CREATE TABLE IF NOT EXISTS evidence (
    id              VARCHAR(128)  NOT NULL PRIMARY KEY,
    schema_version  VARCHAR(32)   NOT NULL,
    source_type     VARCHAR(64)   NOT NULL,
    source_id       VARCHAR(256),
    source_url      VARCHAR(2048),
    captured_at     TIMESTAMP(3)  NOT NULL,
    patch           VARCHAR(64)   NOT NULL,
    sample_size     BIGINT        NOT NULL,
    freshness_hours DOUBLE        NOT NULL,
    reliability     DOUBLE        NOT NULL,
    payload_ref     VARCHAR(512)  NOT NULL
);

CREATE INDEX idx_evidence_patch_id ON evidence (patch, id);

CREATE TABLE IF NOT EXISTS conflict_queue (
    row_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    canonical_id      VARCHAR(128) NOT NULL,
    patch             VARCHAR(64)  NOT NULL,
    left_source_type  VARCHAR(64)  NOT NULL,
    left_source_id    VARCHAR(256),
    right_source_type VARCHAR(64)  NOT NULL,
    right_source_id   VARCHAR(256),
    detected_at       TIMESTAMP(3) NOT NULL,
    summary           TEXT
);

CREATE INDEX idx_conflict_patch_canonical ON conflict_queue (patch, canonical_id);

CREATE TABLE IF NOT EXISTS canonical_entity (
    patch            VARCHAR(64)  NOT NULL,
    canonical_id     VARCHAR(128) NOT NULL,
    kind             VARCHAR(32)  NOT NULL,
    canonical_json   JSON         NOT NULL,
    raw_payload      BLOB,
    raw_source_type  VARCHAR(64),
    raw_source_id    VARCHAR(256),
    PRIMARY KEY (patch, canonical_id)
);

CREATE INDEX idx_canonical_patch_kind ON canonical_entity (patch, kind);

CREATE TABLE IF NOT EXISTS patch_record (
    id           VARCHAR(64)  NOT NULL PRIMARY KEY,
    set_id       VARCHAR(64)  NOT NULL,
    effective_at TIMESTAMP(3) NOT NULL,
    retired_at   TIMESTAMP(3),
    status       VARCHAR(16)  NOT NULL,
    ttl_seconds  BIGINT       NOT NULL,
    is_current   TINYINT(1)   NOT NULL DEFAULT 0
);

CREATE INDEX idx_patch_set_id ON patch_record (set_id, id);
