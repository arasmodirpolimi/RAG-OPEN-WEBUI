package com.ragplatform.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ragplatform.domain.exception.InvalidStateTransitionException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SourceDocumentTest {

    private final Instant now = Instant.parse("2026-10-01T10:00:00Z");

    private SourceDocument pending() {
        return SourceDocument.newlyUploaded(DocumentId.newId(), "contratti", "a.pdf",
                "application/pdf", "abc", "contratti/x/a.pdf", now);
    }

    @Test
    void happyPathPendingProcessingIndexed() {
        SourceDocument indexed = pending().markProcessing().markIndexed(now);
        assertEquals(DocumentStatus.INDEXED, indexed.status());
        assertNotNull(indexed.indexedAt());
    }

    @Test
    void failureKeepsReasonAndCanBeRequeued() {
        SourceDocument failed = pending().markProcessing().markFailed("boom");
        assertEquals("boom", failed.errorMessage());
        SourceDocument requeued = failed.markPending();
        assertEquals(DocumentStatus.PENDING, requeued.status());
        assertNull(requeued.errorMessage());
    }

    @Test
    void cannotIndexWithoutProcessing() {
        assertThrows(InvalidStateTransitionException.class, () -> pending().markIndexed(now));
    }

    @Test
    void cannotRequeueWhileProcessing() {
        assertThrows(InvalidStateTransitionException.class, () -> pending().markProcessing().markPending());
    }

    @Test
    void indexedDocumentMustBeRequeuedExplicitly() {
        SourceDocument indexed = pending().markProcessing().markIndexed(now);
        assertThrows(InvalidStateTransitionException.class, indexed::markProcessing);
    }
}
