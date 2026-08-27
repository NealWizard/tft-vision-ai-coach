package com.tft.coach.data.normalize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.data.entity.EntityKind;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

/** MySQL-backed canonical knowledge store (`P1-DATA-Normalize-001`). */
public final class JdbcCanonicalKnowledgeStore implements CanonicalKnowledgeStore {

    private static final TypeReference<Map<String, Object>> CANONICAL_TYPE = new TypeReference<>() {};

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public JdbcCanonicalKnowledgeStore(DataSource dataSource) {
        this(dataSource, new ObjectMapper());
    }

    public JdbcCanonicalKnowledgeStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void put(NormalizedEntity entity) {
        Objects.requireNonNull(entity, "entity");
        String sql = """
                INSERT INTO canonical_entity (
                    patch, canonical_id, kind, canonical_json,
                    raw_payload, raw_source_type, raw_source_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    kind = VALUES(kind),
                    canonical_json = VALUES(canonical_json),
                    raw_payload = VALUES(raw_payload),
                    raw_source_type = VALUES(raw_source_type),
                    raw_source_id = VALUES(raw_source_id)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.patch());
            ps.setString(2, entity.canonicalId());
            ps.setString(3, entity.kind().name());
            ps.setString(4, objectMapper.writeValueAsString(entity.canonical()));
            ps.setBytes(5, entity.rawPayload());
            ps.setString(6, entity.rawSourceType());
            ps.setString(7, entity.rawSourceId());
            ps.executeUpdate();
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Failed to put canonical entity: " + entity.canonicalId(), ex);
        }
    }

    @Override
    public Optional<NormalizedEntity> get(String patch, String canonicalId) {
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(canonicalId, "canonicalId");
        String sql = """
                SELECT patch, canonical_id, kind, canonical_json,
                       raw_payload, raw_source_type, raw_source_id
                FROM canonical_entity
                WHERE patch = ? AND canonical_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, patch);
            ps.setString(2, canonicalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to get canonical entity: " + canonicalId, ex);
        }
    }

    @Override
    public List<NormalizedEntity> findByKind(String patch, EntityKind kind) {
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(kind, "kind");
        String sql = """
                SELECT patch, canonical_id, kind, canonical_json,
                       raw_payload, raw_source_type, raw_source_id
                FROM canonical_entity
                WHERE patch = ? AND kind = ?
                ORDER BY canonical_id
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, patch);
            ps.setString(2, kind.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<NormalizedEntity> matches = new ArrayList<>();
                while (rs.next()) {
                    matches.add(mapRow(rs));
                }
                return List.copyOf(matches);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find entities by kind: " + kind, ex);
        }
    }

    @Override
    public List<NormalizedEntity> searchByName(String patch, EntityKind kind, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return findByKind(patch, kind).stream()
                .filter(entity -> entity.canonical().getOrDefault("name", "")
                        .toString()
                        .toLowerCase(Locale.ROOT)
                        .contains(needle))
                .toList();
    }

    @Override
    public int size() {
        String sql = "SELECT COUNT(*) FROM canonical_entity";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count canonical entities", ex);
        }
    }

    private NormalizedEntity mapRow(ResultSet rs) throws SQLException {
        try {
            Map<String, Object> canonical = objectMapper.readValue(
                    rs.getString("canonical_json"),
                    CANONICAL_TYPE);
            byte[] rawPayload = rs.getBytes("raw_payload");
            return new NormalizedEntity(
                    rs.getString("canonical_id"),
                    EntityKind.valueOf(rs.getString("kind")),
                    rs.getString("patch"),
                    canonical,
                    rawPayload == null ? new byte[0] : rawPayload,
                    rs.getString("raw_source_type"),
                    rs.getString("raw_source_id"));
        } catch (IOException ex) {
            throw new SQLException("Failed to deserialize canonical JSON", ex);
        }
    }
}
