package com.netflixclone.controller;

import com.netflixclone.dto.request.VideoRequest;
import com.netflixclone.dto.response.PageResponse;
import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public PageResponse<VideoResponse> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return videoService.getPublishedVideos(page, size, category, search);
    }

    @GetMapping("/{id}")
    public VideoResponse getVideo(@PathVariable long id) {
        return videoService.getPublishedVideo(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public VideoResponse createVideo(@Valid @RequestBody VideoRequest request) {
        return videoService.createVideo(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VideoResponse updateVideo(
            @PathVariable long id, @Valid @RequestBody VideoRequest request) {
        return videoService.updateVideo(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteVideo(@PathVariable long id) {
        videoService.deleteVideo(id);
    }
}
