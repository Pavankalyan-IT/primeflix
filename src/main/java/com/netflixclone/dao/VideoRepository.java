package com.netflixclone.dao;

import com.netflixclone.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video,Long> {
    Page<Video> findByPublishedTrue(Pageable pageable);

    Page<Video> findByPublishedTrueAndTitleContainingIgnoreCase(
            String title, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Video v JOIN v.categoryEntities c " +
           "WHERE v.published = true AND LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
           "AND LOWER(c.name) = LOWER(:category)")
    Page<Video> findByPublishedTrueAndTitleContainingIgnoreCaseAndCategoriesContaining(
            @Param("title") String title, @Param("category") String category, Pageable pageable);

    Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Video> findByPublished(boolean published, Pageable pageable);

    Page<Video> findByPublishedAndTitleContainingIgnoreCase(
            boolean published, String title, Pageable pageable);

    long countByPublishedTrue();

    @Query("select coalesce(sum(v.duration), 0) from Video v")
    long sumDuration();

    @Query("SELECT DISTINCT v FROM Video v JOIN v.categoryEntities c " +
           "WHERE v.published = true AND LOWER(c.name) = LOWER(:category)")
    Page<Video> findDistinctByPublishedTrueAndCategoriesContaining(
            @Param("category") String category, Pageable pageable);

    Optional<Video> findBySrcUuidAndPublishedTrue(String srcUuid);

    Optional<Video> findByPosterUuidAndPublishedTrue(String posterUuid);

    Optional<Video> findBySrcUuid(String srcUuid);

    Optional<Video> findByPosterUuid(String posterUuid);
}
