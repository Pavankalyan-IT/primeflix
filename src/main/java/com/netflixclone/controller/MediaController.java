package com.netflixclone.controller;

import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.service.MediaService;
import com.netflixclone.service.StoredMedia;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/videos/{id}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public VideoResponse uploadVideo(
            @PathVariable long id, @RequestParam("file") MultipartFile file) throws IOException {
        return mediaService.uploadVideo(id, file);
    }

    @PostMapping(value = "/videos/{id}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public VideoResponse uploadPoster(
            @PathVariable long id, @RequestParam("file") MultipartFile file) throws IOException {
        return mediaService.uploadPoster(id, file);
    }

    @GetMapping("/files/video/{id}")
    public ResponseEntity<StreamingResponseBody> getVideo(
            @PathVariable String id, HttpServletRequest request) throws IOException {
        StoredMedia media = mediaService.getPublishedVideo(id);
        return streamMedia(media, request.getHeader(HttpHeaders.RANGE));
    }

    @GetMapping("/files/image/{id}")
    public ResponseEntity<Resource> getPoster(@PathVariable String id) throws IOException {
        StoredMedia media = mediaService.getPublishedPoster(id);
        return ResponseEntity.ok()
                .contentType(media.contentType())
                .contentLength(Files.size(media.path()))
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(media.path()));
    }

    @GetMapping("/admin/media/video/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StreamingResponseBody> getAdminVideo(
            @PathVariable String id, HttpServletRequest request) throws IOException {
        StoredMedia media = mediaService.getAdminVideo(id);
        return streamMedia(media, request.getHeader(HttpHeaders.RANGE));
    }

    @GetMapping("/admin/media/image/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> getAdminPoster(@PathVariable String id) throws IOException {
        StoredMedia media = mediaService.getAdminPoster(id);
        return ResponseEntity.ok()
                .contentType(media.contentType())
                .contentLength(Files.size(media.path()))
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(media.path()));
    }

    private ResponseEntity<StreamingResponseBody> streamMedia(StoredMedia media, String rangeHeader)
            throws IOException {
        long fileLength = Files.size(media.path());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(media.contentType());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set("X-Content-Type-Options", "nosniff");

        long start = 0;
        long end = fileLength - 1;
        HttpStatus status = HttpStatus.OK;

        if (rangeHeader != null) {
            List<HttpRange> ranges;
            try {
                ranges = HttpRange.parseRanges(rangeHeader);
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                        .build();
            }
            if (ranges.size() != 1 || fileLength == 0) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                        .build();
            }
            HttpRange range = ranges.get(0);
            try {
                start = range.getRangeStart(fileLength);
                end = range.getRangeEnd(fileLength);
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                        .build();
            }
            if (start >= fileLength || start > end) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                        .build();
            }
            status = HttpStatus.PARTIAL_CONTENT;
            headers.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + start + "-" + end + "/" + fileLength);
        }

        long contentLength = end - start + 1;
        headers.setContentLength(contentLength);
        long streamStart = start;
        StreamingResponseBody body = output -> copyRange(media.path(), streamStart, contentLength, output);
        return new ResponseEntity<>(body, headers, status);
    }

    private void copyRange(java.nio.file.Path path, long start, long length, OutputStream output)
            throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            input.skipNBytes(start);
            byte[] buffer = new byte[64 * 1024];
            long remaining = length;
            while (remaining > 0) {
                int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                output.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }
}
