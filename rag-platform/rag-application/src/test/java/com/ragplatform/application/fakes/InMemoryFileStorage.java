package com.ragplatform.application.fakes;

import com.ragplatform.application.port.out.FileStoragePort;
import java.util.HashMap;
import java.util.Map;

public final class InMemoryFileStorage implements FileStoragePort {

    public final Map<String, byte[]> files = new HashMap<>();

    @Override
    public void store(String key, byte[] content) {
        files.put(key, content.clone());
    }

    @Override
    public byte[] load(String key) {
        byte[] content = files.get(key);
        if (content == null) {
            throw new IllegalStateException("missing " + key);
        }
        return content.clone();
    }
}
