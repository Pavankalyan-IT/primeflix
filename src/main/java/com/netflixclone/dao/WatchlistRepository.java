package com.netflixclone.dao;

import com.netflixclone.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserIdOrderByAddedAtDesc(long userId);

    Optional<Watchlist> findByUserIdAndVideoId(long userId, long videoId);

    boolean existsByUserIdAndVideoId(long userId, long videoId);

    @Modifying
    @Query("DELETE FROM Watchlist w WHERE w.user.id = :userId")
    int deleteByUserId(@Param("userId") long userId);

    @Modifying
    @Query("DELETE FROM Watchlist w WHERE w.video.id = :videoId")
    int deleteByVideoId(@Param("videoId") long videoId);
}
