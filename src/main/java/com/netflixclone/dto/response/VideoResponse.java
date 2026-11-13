package com.netflixclone.dto.response;

import com.netflixclone.entity.Video;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.Instant;

@Data
@NoArgsConstructor

public class VideoResponse {
    private long id;
    private String title;
    private String description;
    private Integer year;
    private String rating;

    private Integer duration;
    private String src;
    private String poster;
    private boolean published;

    private List<String>categories;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isInWatchlist;

    public VideoResponse(Long id,
                         String title,
                         String description,
                         Integer year,
                         String rating,
                         Integer duration,
                         String src,
                         String poster,
                         boolean published,
                         List<String> categories,
                         Instant createdAt,
                         Instant updatedAt) {
       this.id=id;
       this.title=title;
       this.description=description;
       this.year=year;
       this.rating=rating;
       this.duration=duration;
       this.src=src;
       this.poster=poster;
       this.published =published;
       this.categories=categories;
       this.createdAt = createdAt;
       this.updatedAt=updatedAt;
    }
    public static VideoResponse fromEntity(Video video){
        VideoResponse response = new VideoResponse(video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getYear(),
                video.getRating(),
                video.getDuration(),
                video.getSrc(),
                video.getPoster(),
                video.isPublished(),
                video.getCategories(),
                video.getCreatedAt(),
                video.getUpdatedAt());
        if(video.getIsInWatchList() != null){
            response.setIsInWatchlist(video.getIsInWatchList());
        }
        return response;
    }


}
