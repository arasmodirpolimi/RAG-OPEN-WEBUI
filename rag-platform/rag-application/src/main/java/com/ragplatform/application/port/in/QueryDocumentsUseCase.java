package com.ragplatform.application.port.in;

import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.SourceDocument;
import java.util.List;

public interface QueryDocumentsUseCase {

    SourceDocument get(DocumentId id);

    List<SourceDocument> listByCollection(String collection);
}
