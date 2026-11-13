package com.netflixclone.service;

import com.netflixclone.dao.VideoRepository;
import com.netflixclone.dao.WatchlistRepository;
import com.netflixclone.dto.request.VideoRequest;
import com.netflixclone.dto.response.PageResponse;
import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.entity.Video;
import com.netflixclone.exception.ResourceNotFoundException;
import com.netflixclone.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final WatchlistRepository watchlistRepository;

    public VideoService(VideoRepository videoRepository, WatchlistRepository watchlistRepository) {
        this.videoRepository = videoRepository;
        this.watchlistRepository = watchlistRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getPublishedVideos(
            int page, int size, String category, String search) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasSearch = search != null && !search.isBlank();
        Page<Video> videos;
        if (hasCategory && hasSearch) {
            videos = videoRepository.findByPublishedTrueAndTitleContainingIgnoreCaseAndCategoriesContaining(
                    search.trim(), category.trim(), pageable);
        } else if (hasCategory) {
            videos = videoRepository.findDistinctByPublishedTrueAndCategoriesContaining(
                    category.trim(), pageable);
        } else if (hasSearch) {
            videos = videoRepository.findByPublishedTrueAndTitleContainingIgnoreCase(
                    search.trim(), pageable);
        } else {
            videos = videoRepository.findByPublishedTrue(pageable);
        }
        return PaginationUtils.toPageResponse(videos.map(VideoResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public VideoResponse getPublishedVideo(long id) {
        Video video = videoRepository.findById(id)
                .filter(Video::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Published video not found with ID: " + id));
        return VideoResponse.fromEntity(video);
    }

    @Transactional
    public VideoResponse createVideo(VideoRequest request) {
        Video video = new Video();
        updateFields(video, request);
        return VideoResponse.fromEntity(videoRepository.save(video));
    }

    @Transactional
    public VideoResponse updateVideo(long id, VideoRequest request) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Video not found with ID: " + id));
        updateFields(video, request);
        return VideoResponse.fromEntity(videoRepository.save(video));
    }

    @Transactional
    public void deleteVideo(long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Video not found with ID: " + id));
        watchlistRepository.deleteByVideoId(id);
        videoRepository.delete(video);
    }

    private void updateFields(Video video, VideoRequest request) {
        video.setTitle(request.getTitle().trim());
        video.setDescription(request.getDescription());
        video.setYear(request.getYear());
        video.setRating(request.getRating());
        video.setDuration(request.getDuration());
        video.setPublished(request.isPublished());
        if (video.isPublished() && (video.getSrcUuid() == null || video.getSrcUuid().isBlank())) {
            throw new IllegalArgumentException("Upload the video file before publishing");
        }
        video.setCategories(request.getCategories() == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(request.getCategories()));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}
