package com.netflixclone.service;

import com.netflixclone.dao.VideoRepository;
import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.entity.Video;
import com.netflixclone.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

@Service
public class MediaService {

    private final VideoRepository videoRepository;
    private final MediaStorageService mediaStorageService;

    public MediaService(VideoRepository videoRepository, MediaStorageService mediaStorageService) {
        this.videoRepository = videoRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public VideoResponse uploadVideo(long videoId, MultipartFile file) throws IOException {
        Video video = getVideo(videoId);
        video.setSrcUuid(mediaStorageService.storeVideo(file));
        return VideoResponse.fromEntity(videoRepository.save(video));
    }

    @Transactional
    public VideoResponse uploadPoster(long videoId, MultipartFile file) throws IOException {
        Video video = getVideo(videoId);
        video.setPosterUuid(mediaStorageService.storeImage(file));
        return VideoResponse.fromEntity(videoRepository.save(video));
    }

    @Transactional(readOnly = true)
    public StoredMedia getPublishedVideo(String id) throws IOException {
        videoRepository.findBySrcUuidAndPublishedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Published video file was not found"));
        return mediaStorageService.getVideo(id);
    }

    @Transactional(readOnly = true)
    public StoredMedia getPublishedPoster(String id) throws IOException {
        videoRepository.findByPosterUuidAndPublishedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Published poster was not found"));
        return mediaStorageService.getImage(id);
    }

    @Transactional(readOnly = true)
    public StoredMedia getAdminVideo(String id) throws IOException {
        videoRepository.findBySrcUuid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video file was not found"));
        return mediaStorageService.getVideo(id);
    }

    @Transactional(readOnly = true)
    public StoredMedia getAdminPoster(String id) throws IOException {
        videoRepository.findByPosterUuid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poster was not found"));
        return mediaStorageService.getImage(id);
    }

    private Video getVideo(long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with ID: " + id));
    }
}
