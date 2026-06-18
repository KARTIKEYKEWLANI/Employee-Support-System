package com.support.ticketing.service.impl;

import com.support.ticketing.service.StorageService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${app.storage.dir:uploads}") String rootDir) throws Exception {
        this.root = Path.of(rootDir).toAbsolutePath();
        Files.createDirectories(this.root);
    }

    @Override
    public String save(String originalFilename, InputStream content) throws Exception {
        String safeName = originalFilename == null ? "file" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + "_" + safeName;
        Path target = root.resolve(storedName);
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    @Override
    public Path load(String storagePath) {
        return Path.of(storagePath);
    }
}
