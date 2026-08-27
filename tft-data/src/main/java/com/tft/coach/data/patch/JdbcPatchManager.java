package com.tft.coach.data.patch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

/** MySQL-backed patch lifecycle manager (`P1-DATA-Patch-001`). */
public final class JdbcPatchManager implements PatchManager {

    private final DataSource dataSource;
    private volatile String cachedCurrentPatchId;

    public JdbcPatchManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void register(PatchRecord patch) {
        Objects.requireNonNull(patch, "patch");
        String upsert = """
                INSERT INTO patch_record (
                    id, set_id, effective_at, retired_at, status, ttl_seconds, is_current
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    set_id = VALUES(set_id),
                    effective_at = VALUES(effective_at),
                    retired_at = VALUES(retired_at),
                    status = VALUES(status),
                    ttl_seconds = VALUES(ttl_seconds),
                    is_current = VALUES(is_current)
                """;
        boolean markCurrent = patch.status() == PatchStatus.CURRENT || !hasCurrentPatch();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (markCurrent) {
                    clearCurrentFlag(connection);
                }
                try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                    ps.setString(1, patch.id());
                    ps.setString(2, patch.setId());
                    ps.setTimestamp(3, Timestamp.from(patch.effectiveAt()));
                    if (patch.retiredAt() == null) {
                        ps.setNull(4, java.sql.Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(4, Timestamp.from(patch.retiredAt()));
                    }
                    ps.setString(5, patch.status().name());
                    ps.setLong(6, patch.ttl().getSeconds());
                    ps.setBoolean(7, markCurrent);
                    ps.executeUpdate();
                }
                connection.commit();
                if (markCurrent) {
                    cachedCurrentPatchId = patch.id();
                }
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to register patch: " + patch.id(), ex);
        }
    }

    @Override
    public String currentPatch() {
        if (cachedCurrentPatchId != null) {
            return cachedCurrentPatchId;
        }
        String sql = "SELECT id FROM patch_record WHERE is_current = 1 LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw PatchRequiredException.missing();
            }
            cachedCurrentPatchId = rs.getString("id");
            return cachedCurrentPatchId;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to resolve current patch", ex);
        }
    }

    @Override
    public PatchRecord require(String patchId) {
        if (patchId == null || patchId.isBlank()) {
            throw PatchRequiredException.missing();
        }
        PatchRecord patch = find(patchId).orElseThrow(() -> PatchRequiredException.unknown(patchId));
        if (patch.isExpired(Instant.now())) {
            throw PatchRequiredException.unknown(patchId);
        }
        return patch;
    }

    @Override
    public Optional<PatchRecord> find(String patchId) {
        if (patchId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, set_id, effective_at, retired_at, status, ttl_seconds
                FROM patch_record WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, patchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find patch: " + patchId, ex);
        }
    }

    private static void clearCurrentFlag(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE patch_record SET is_current = 0 WHERE is_current = 1")) {
            ps.executeUpdate();
        }
    }

    private boolean hasCurrentPatch() {
        if (cachedCurrentPatchId != null) {
            return true;
        }
        String sql = "SELECT 1 FROM patch_record WHERE is_current = 1 LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to check current patch", ex);
        }
    }

    private static PatchRecord mapRow(ResultSet rs) throws SQLException {
        Timestamp retiredAt = rs.getTimestamp("retired_at");
        return new PatchRecord(
                rs.getString("id"),
                rs.getString("set_id"),
                rs.getTimestamp("effective_at").toInstant(),
                retiredAt == null ? null : retiredAt.toInstant(),
                PatchStatus.valueOf(rs.getString("status")),
                Duration.ofSeconds(rs.getLong("ttl_seconds")));
    }
}
