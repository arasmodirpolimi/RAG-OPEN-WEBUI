package com.ragplatform.api.web.documents;

import com.ragplatform.application.port.in.QueryDocumentsUseCase;
import com.ragplatform.application.port.in.ReindexDocumentUseCase;
import com.ragplatform.application.port.in.UploadDocumentUseCase;
import com.ragplatform.application.port.in.UploadDocumentUseCase.UploadDocumentCommand;
import com.ragplatform.domain.model.DocumentId;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final UploadDocumentUseCase upload;
    private final ReindexDocumentUseCase reindex;
    private final QueryDocumentsUseCase query;

    public DocumentController(UploadDocumentUseCase upload, ReindexDocumentUseCase reindex,
                              QueryDocumentsUseCase query) {
        this.upload = upload;
        this.reindex = reindex;
        this.query = query;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("collection") String collection)
            throws IOException {
        DocumentId id = upload.upload(new UploadDocumentCommand(collection,
                file.getOriginalFilename(), file.getContentType(), file.getBytes()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id.toString(), "status", "PENDING"));
    }

    @GetMapping("/{id}")
    public DocumentView get(@PathVariable("id") String id) {
        return DocumentView.of(query.get(DocumentId.of(id)));
    }

    @GetMapping
    public List<DocumentView> list(@RequestParam("collection") String collection) {
        return query.listByCollection(collection).stream().map(DocumentView::of).toList();
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<Map<String, String>> reindex(@PathVariable("id") String id) {
        reindex.reindex(DocumentId.of(id));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id, "status", "PENDING"));
    }
}
