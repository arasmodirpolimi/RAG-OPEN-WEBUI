package com.ragplatform.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.application.fakes.InMemoryDocumentRepository;
import com.ragplatform.application.fakes.InMemoryFileStorage;
import com.ragplatform.application.port.in.UploadDocumentUseCase.UploadDocumentCommand;
import com.ragplatform.domain.exception.DuplicateDocumentException;
import com.ragplatform.domain.exception.UnknownCollectionException;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.DocumentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UploadDocumentServiceTest {

    private final InMemoryDocumentRepository repo = new InMemoryDocumentRepository();
    private final InMemoryFileStorage storage = new InMemoryFileStorage();
    private final List<DocumentId> queued = new ArrayList<>();
    private final UploadDocumentService service = new UploadDocumentService(repo, storage, queued::add,
            new CollectionPolicy(List.of("contratti")), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    private UploadDocumentCommand cmd(String collection, byte[] bytes) {
        return new UploadDocumentCommand(collection, "../Contratto 2026.pdf", "application/pdf", bytes);
    }

    @Test
    void storesSavesPendingAndEnqueues() {
        DocumentId id = service.upload(cmd("contratti", new byte[] {1, 2, 3}));

        assertEquals(DocumentStatus.PENDING, repo.store.get(id).status());
        assertEquals(List.of(id), queued);
        String key = repo.store.get(id).storageKey();
        assertTrue(key.endsWith("/Contratto_2026.pdf"), "file name is sanitised: " + key);
        assertTrue(storage.files.containsKey(key));
    }

    @Test
    void rejectsDuplicateContentInSameCollection() {
        DocumentId first = service.upload(cmd("contratti", new byte[] {9}));
        DuplicateDocumentException e = assertThrows(DuplicateDocumentException.class,
                () -> service.upload(cmd("contratti", new byte[] {9})));
        assertEquals(first, e.existingId());
        assertEquals(1, queued.size());
    }

    @Test
    void rejectsUnknownCollection() {
        assertThrows(UnknownCollectionException.class, () -> service.upload(cmd("altro", new byte[] {1})));
    }
}
