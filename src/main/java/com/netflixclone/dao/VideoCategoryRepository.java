package com.netflixclone.dao;

import com.netflixclone.entity.VideoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoCategoryRepository extends JpaRepository<VideoCategory, Long> {

    List<VideoCategory> findByVideoId(long videoId);

    boolean existsByVideoIdAndNameIgnoreCase(long videoId, String name);

    @Query("SELECT DISTINCT v.video.id FROM VideoCategory v WHERE v.name = :category")
    List<Long> findVideoIdsByCategory(@Param("category") String category);
}
