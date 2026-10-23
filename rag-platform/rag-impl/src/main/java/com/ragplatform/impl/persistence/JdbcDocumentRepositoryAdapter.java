package com.ragplatform.impl.persistence;

import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.DocumentStatus;
import com.ragplatform.domain.model.SourceDocument;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL via Spring JdbcClient. Schema: db/migration/V1__documents.sql (Flyway). */
public final class JdbcDocumentRepositoryAdapter implements DocumentRepositoryPort {

    private static final String COLUMNS = "id, collection, file_name, mime_type, sha256, storage_key, "
            + "status, error_message, created_at, indexed_at";

    private final JdbcClient jdbc;

    public JdbcDocumentRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(SourceDocument d) {
        jdbc.sql("INSERT INTO documents (" + COLUMNS + ") VALUES (:id, :collection, :fileName, :mimeType, "
                        + ":sha256, :storageKey, :status, :error, :createdAt, :indexedAt) "
                        + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, "
                        + "error_message = EXCLUDED.error_message, indexed_at = EXCLUDED.indexed_at")
                .param("id", d.id().value())
                .param("collection", d.collection())
                .param("fileName", d.fileName())
                .param("mimeType", d.mimeType(), Types.VARCHAR)
                .param("sha256", d.sha256())
                .param("storageKey", d.storageKey())
                .param("status", d.status().name())
                .param("error", d.errorMessage(), Types.VARCHAR)
                .param("createdAt", toOffset(d.createdAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("indexedAt", toOffset(d.indexedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
    }

    @Override
    public Optional<SourceDocument> findById(DocumentId id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM documents WHERE id = :id")
                .param("id", id.value())
                .query(JdbcDocumentRepositoryAdapter::map).optional();
    }

    @Override
    public Optional<SourceDocument> findByCollectionAndSha256(String collection, String sha256) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM documents WHERE collection = :c AND sha256 = :s")
                .param("c", collection).param("s", sha256)
                .query(JdbcDocumentRepositoryAdapter::map).optional();
    }

    @Override
    public List<SourceDocument> findByCollection(String collection) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM documents WHERE collection = :c ORDER BY created_at DESC")
                .param("c", collection)
                .query(JdbcDocumentRepositoryAdapter::map).list();
    }

    private static SourceDocument map(ResultSet rs, int row) throws SQLException {
        OffsetDateTime indexed = rs.getObject("indexed_at", OffsetDateTime.class);
        return new SourceDocument(
                new DocumentId(rs.getObject("id", UUID.class)),
                rs.getString("collection"),
                rs.getString("file_name"),
                rs.getString("mime_type"),
                rs.getString("sha256"),
                rs.getString("storage_key"),
                DocumentStatus.valueOf(rs.getString("status")),
                rs.getString("error_message"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                indexed == null ? null : indexed.toInstant());
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
