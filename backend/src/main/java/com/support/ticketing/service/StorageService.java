package com.support.ticketing.service;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {
    String save(String originalFilename, InputStream content) throws Exception;

    Path load(String storagePath);
}
