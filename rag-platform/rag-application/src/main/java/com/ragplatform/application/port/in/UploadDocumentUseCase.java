package com.ragplatform.application.port.in;

import com.ragplatform.domain.model.DocumentId;

public interface UploadDocumentUseCase {

    DocumentId upload(UploadDocumentCommand command);

    record UploadDocumentCommand(String collection, String fileName, String mimeType, byte[] content) {
        public UploadDocumentCommand {
            if (content == null || content.length == 0) {
                throw new IllegalArgumentException("File is empty");
            }
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("File name is required");
            }
        }
    }
}
