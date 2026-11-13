package com.netflixclone.service;

import com.netflixclone.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Map<String, String> VIDEO_TYPES = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/ogg", "ogv",
            "video/quicktime", "mov"
    );
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path videoDirectory;
    private final Path imageDirectory;
    private static final long MAX_VIDEO_BYTES = 2L * 1024 * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    public MediaStorageService(
            @Value("${file.upload.video-dir:uploads/videos}") String videoDirectory,
            @Value("${file.upload.image-dir:uploads/images}") String imageDirectory) {
        this.videoDirectory = Path.of(videoDirectory).toAbsolutePath().normalize();
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void createDirectories() throws IOException {
        Files.createDirectories(videoDirectory);
        Files.createDirectories(imageDirectory);
    }

    public String storeVideo(MultipartFile file) throws IOException {
        return store(file, videoDirectory, VIDEO_TYPES);
    }

    public String storeImage(MultipartFile file) throws IOException {
        return store(file, imageDirectory, IMAGE_TYPES);
    }

    public StoredMedia getVideo(String id) throws IOException {
        return find(id, videoDirectory, VIDEO_TYPES);
    }

    public StoredMedia getImage(String id) throws IOException {
        return find(id, imageDirectory, IMAGE_TYPES);
    }

    private String store(
            MultipartFile file, Path directory, Map<String, String> allowedTypes)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file must not be empty");
        }

        String extension = allowedTypes.get(normalizeContentType(file.getContentType()));
        if (extension == null) {
            throw new IllegalArgumentException("Unsupported media file type");
        }
        long maxBytes = allowedTypes == VIDEO_TYPES ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    allowedTypes == VIDEO_TYPES
                            ? "Video file must be no larger than 2 GB"
                            : "Poster image must be no larger than 10 MB");
        }
        if (!hasValidSignature(file, allowedTypes == VIDEO_TYPES)) {
            throw new IllegalArgumentException("File contents do not match the selected media type");
        }

        String id = UUID.randomUUID().toString();
        Path target = directory.resolve(id + "." + extension).normalize();
        if (!target.getParent().equals(directory)) {
            throw new IllegalArgumentException("Invalid media file path");
        }
        file.transferTo(target);
        return id;
    }

    private boolean hasValidSignature(MultipartFile file, boolean video) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            String contentType = normalizeContentType(file.getContentType());
            if (video) {
                return switch (contentType) {
                    case "video/mp4" -> containsAscii(header, 4, "ftyp");
                    case "video/quicktime" -> containsAscii(header, 4, "ftyp");
                    case "video/webm" -> startsWith(header, new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3});
                    case "video/ogg" -> startsWith(header, new byte[]{'O', 'g', 'g', 'S'});
                    default -> false;
                };
            }
            return switch (contentType) {
                case "image/jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                case "image/png" -> startsWith(header,
                        new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
                case "image/webp" -> containsAscii(header, 0, "RIFF")
                        && containsAscii(header, 8, "WEBP");
                default -> false;
            };
        }
    }

    private boolean startsWith(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (actual[index] != expected[index]) return false;
        }
        return true;
    }

    private boolean containsAscii(byte[] actual, int offset, String expected) {
        if (actual.length < offset + expected.length()) return false;
        for (int index = 0; index < expected.length(); index++) {
            if (actual[offset + index] != (byte) expected.charAt(index)) return false;
        }
        return true;
    }

    private StoredMedia find(
            String id,
            Path directory,
            Map<String, String> allowedTypes) throws IOException {
        final UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid media identifier");
        }

        String normalizedId = uuid.toString();
        for (Map.Entry<String, String> entry : allowedTypes.entrySet()) {
            Path candidate = directory.resolve(normalizedId + "." + entry.getValue()).normalize();
            if (candidate.getParent().equals(directory) && Files.isRegularFile(candidate)) {
                return new StoredMedia(candidate, MediaType.parseMediaType(entry.getKey()));
            }
        }
        throw new ResourceNotFoundException("Media file was not found");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameterIndex = contentType.indexOf(';');
        return (parameterIndex >= 0 ? contentType.substring(0, parameterIndex) : contentType)
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
    }
}
