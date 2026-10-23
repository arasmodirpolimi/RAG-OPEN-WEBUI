package com.ragplatform.application.port.out;

/** Durable storage of original files; the search index can always be rebuilt from here. */
public interface FileStoragePort {

    void store(String key, byte[] content);

    byte[] load(String key);
}
