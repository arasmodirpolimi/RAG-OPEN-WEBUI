package com.ragplatform.impl.storage;

import com.ragplatform.application.port.out.FileStoragePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores originals on a (Docker volume) filesystem. For production, add an S3-compatible adapter
 * implementing the same port; no other code changes.
 */
public final class LocalFileStorageAdapter implements FileStoragePort {

    private final Path root;

    public LocalFileStorageAdapter(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            Files.write(tmp, content);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store " + key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + key, e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return path;
    }
}
