package com.tft.coach.data.conflict;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

/** MySQL-backed conflict queue (`P1-DATA-Conflict-001`). */
public final class JdbcConflictQueue implements ConflictQueue {

    private final DataSource dataSource;

    public JdbcConflictQueue(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void enqueue(ConflictRecord record) {
        Objects.requireNonNull(record, "record");
        String sql = """
                INSERT INTO conflict_queue (
                    canonical_id, patch, left_source_type, left_source_id,
                    right_source_type, right_source_id, detected_at, summary
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, record.canonicalId());
            ps.setString(2, record.patch());
            ps.setString(3, record.leftSourceType());
            ps.setString(4, record.leftSourceId());
            ps.setString(5, record.rightSourceType());
            ps.setString(6, record.rightSourceId());
            ps.setTimestamp(7, Timestamp.from(record.detectedAt()));
            ps.setString(8, record.summary());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to enqueue conflict: " + record.canonicalId(), ex);
        }
    }

    @Override
    public List<ConflictRecord> snapshot() {
        String sql = """
                SELECT canonical_id, patch, left_source_type, left_source_id,
                       right_source_type, right_source_id, detected_at, summary
                FROM conflict_queue
                ORDER BY row_id
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ConflictRecord> records = new ArrayList<>();
            while (rs.next()) {
                records.add(mapRow(rs));
            }
            return List.copyOf(records);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to snapshot conflict queue", ex);
        }
    }

    @Override
    public int size() {
        String sql = "SELECT COUNT(*) FROM conflict_queue";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count conflict queue rows", ex);
        }
    }

    private static ConflictRecord mapRow(ResultSet rs) throws SQLException {
        Instant detectedAt = rs.getTimestamp("detected_at").toInstant();
        return new ConflictRecord(
                rs.getString("canonical_id"),
                rs.getString("patch"),
                rs.getString("left_source_type"),
                rs.getString("left_source_id"),
                rs.getString("right_source_type"),
                rs.getString("right_source_id"),
                detectedAt,
                rs.getString("summary"));
    }
}
