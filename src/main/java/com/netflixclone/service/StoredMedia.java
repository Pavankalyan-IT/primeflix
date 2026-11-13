package com.netflixclone.service;

import org.springframework.http.MediaType;

import java.nio.file.Path;

public record StoredMedia(Path path, MediaType contentType) {
}
