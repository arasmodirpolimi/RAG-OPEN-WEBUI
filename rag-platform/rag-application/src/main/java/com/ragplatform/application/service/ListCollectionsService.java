package com.ragplatform.application.service;

import com.ragplatform.application.port.in.ListCollectionsUseCase;
import java.util.List;

public final class ListCollectionsService implements ListCollectionsUseCase {

    private final CollectionPolicy collections;

    public ListCollectionsService(CollectionPolicy collections) {
        this.collections = collections;
    }

    @Override
    public List<String> collections() {
        return collections.all();
    }
}
