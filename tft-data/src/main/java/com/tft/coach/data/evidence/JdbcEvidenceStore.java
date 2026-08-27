package com.tft.coach.data.evidence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

/** MySQL-backed evidence store (`P1-DATA-Evidence-001`). */
public final class JdbcEvidenceStore implements EvidenceStore {

    private final DataSource dataSource;

    public JdbcEvidenceStore(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void save(EvidenceRecord record) {
        Objects.requireNonNull(record, "record");
        String sql = """
                INSERT INTO evidence (
                    id, schema_version, source_type, source_id, source_url,
                    captured_at, patch, sample_size, freshness_hours, reliability, payload_ref
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    schema_version = VALUES(schema_version),
                    source_type = VALUES(source_type),
                    source_id = VALUES(source_id),
                    source_url = VALUES(source_url),
                    captured_at = VALUES(captured_at),
                    patch = VALUES(patch),
                    sample_size = VALUES(sample_size),
                    freshness_hours = VALUES(freshness_hours),
                    reliability = VALUES(reliability),
                    payload_ref = VALUES(payload_ref)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, record.id());
            ps.setString(2, record.schemaVersion());
            ps.setString(3, record.sourceType());
            ps.setString(4, record.sourceId());
            ps.setString(5, record.sourceUrl());
            ps.setTimestamp(6, Timestamp.from(record.capturedAt()));
            ps.setString(7, record.patch());
            ps.setLong(8, record.sampleSize());
            ps.setDouble(9, record.freshnessHours());
            ps.setDouble(10, record.reliability());
            ps.setString(11, record.payloadRef());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save evidence: " + record.id(), ex);
        }
    }

    @Override
    public Optional<EvidenceRecord> findById(String id) {
        Objects.requireNonNull(id, "id");
        String sql = """
                SELECT schema_version, id, source_type, source_id, source_url,
                       captured_at, patch, sample_size, freshness_hours, reliability, payload_ref
                FROM evidence WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find evidence: " + id, ex);
        }
    }

    @Override
    public int size() {
        String sql = "SELECT COUNT(*) FROM evidence";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count evidence rows", ex);
        }
    }

    private static EvidenceRecord mapRow(ResultSet rs) throws SQLException {
        Instant capturedAt = rs.getTimestamp("captured_at").toInstant();
        return new EvidenceRecord(
                rs.getString("schema_version"),
                rs.getString("id"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getString("source_url"),
                capturedAt,
                rs.getString("patch"),
                rs.getLong("sample_size"),
                rs.getDouble("freshness_hours"),
                rs.getDouble("reliability"),
                rs.getString("payload_ref"));
    }
}
