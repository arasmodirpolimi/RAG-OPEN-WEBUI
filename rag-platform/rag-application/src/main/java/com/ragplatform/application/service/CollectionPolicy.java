package com.ragplatform.application.service;

import com.ragplatform.domain.exception.UnknownCollectionException;
import java.util.List;
import java.util.Set;

/** The configured set of collections (knowledge bases). Single place that validates names. */
public final class CollectionPolicy {

    private final List<String> collections;
    private final Set<String> lookup;

    public CollectionPolicy(List<String> collections) {
        if (collections == null || collections.isEmpty()) {
            throw new IllegalArgumentException("At least one collection must be configured");
        }
        for (String c : collections) {
            if (!c.matches("[a-z0-9][a-z0-9_-]{0,62}")) {
                throw new IllegalArgumentException("Invalid collection name: " + c);
            }
        }
        this.collections = List.copyOf(collections);
        this.lookup = Set.copyOf(collections);
    }

    public void requireKnown(String collection) {
        if (collection == null || !lookup.contains(collection)) {
            throw new UnknownCollectionException(collection);
        }
    }

    public List<String> all() {
        return collections;
    }
}
