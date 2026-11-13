package com.netflixclone.util;

import com.netflixclone.exception.ResourceNotFoundException;
import com.netflixclone.dao.UserRepository;
import com.netflixclone.dao.VideoRepository;
import com.netflixclone.entity.User;
import com.netflixclone.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public ServiceUtil(UserRepository userRepository, VideoRepository videoRepository) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    public User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User getUserByIdOrThrow(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    public Video getVideoByIdOrThrow(long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with ID: " + id));
    }
}