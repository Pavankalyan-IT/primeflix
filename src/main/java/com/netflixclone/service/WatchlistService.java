package com.netflixclone.service;

import com.netflixclone.dao.UserRepository;
import com.netflixclone.dao.VideoRepository;
import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.entity.User;
import com.netflixclone.entity.Video;
import com.netflixclone.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public WatchlistService(UserRepository userRepository, VideoRepository videoRepository) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getWatchlist(String email) {
        User user = getUser(email);
        return user.getWatchlist().stream()
                .map(this::toWatchlistResponse)
                .toList();
    }

    @Transactional
    public VideoResponse addVideo(String email, long videoId) {
        User user = getUser(email);
        Video video = videoRepository.findById(videoId)
                .filter(Video::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Published video not found with ID: " + videoId));
        user.addToWatchlist(video);
        return toWatchlistResponse(video);
    }

    @Transactional
    public void removeVideo(String email, long videoId) {
        User user = getUser(email);
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Video not found with ID: " + videoId));
        user.removeFromWatchlist(video);
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    private VideoResponse toWatchlistResponse(Video video) {
        video.setIsInWatchList(true);
        return VideoResponse.fromEntity(video);
    }
}
