package com.netflixclone.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStorageServiceTest {

    @TempDir
    Path tempDirectory;

    private MediaStorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new MediaStorageService(
                tempDirectory.resolve("videos").toString(),
                tempDirectory.resolve("images").toString());
        storage.createDirectories();
    }

    @Test
    void storesVideoUnderGeneratedIdAndRetrievesItsMediaType() throws Exception {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "movie.mp4", "video/mp4",
                new byte[]{0, 0, 0, 8, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});

        String id = storage.storeVideo(upload);
        StoredMedia stored = storage.getVideo(id);

        assertEquals("video/mp4", stored.contentType().toString());
        assertArrayEquals(new byte[]{0, 0, 0, 8, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'},
                Files.readAllBytes(stored.path()));
        assertTrue(stored.path().getFileName().toString().endsWith(".mp4"));
    }

    @Test
    void rejectsEmptyFilesAndUnsupportedContentTypes() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.mp4", "video/mp4", new byte[0]);
        MockMultipartFile unsupported = new MockMultipartFile(
                "file", "script.svg", "image/svg+xml", new byte[]{1});
        MockMultipartFile fakeVideo = new MockMultipartFile(
                "file", "fake.mp4", "video/mp4", new byte[]{1, 2, 3, 4});

        assertThrows(IllegalArgumentException.class, () -> storage.storeVideo(empty));
        assertThrows(IllegalArgumentException.class, () -> storage.storeImage(unsupported));
        assertThrows(IllegalArgumentException.class, () -> storage.storeVideo(fakeVideo));
        assertThrows(IllegalArgumentException.class, () -> storage.getVideo(".."));
    }
}
