package com.netflixclone.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "watchlist")
@Getter
@Setter
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "datetime")
    private Instant addedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Watchlist watchlist = (Watchlist) o;
        return user != null && video != null
                && user.getId().equals(watchlist.user.getId())
                && video.getId().equals(watchlist.video.getId());
    }

    @Override
    public int hashCode() {
        return 31 * user.getId().hashCode() + video.getId().hashCode();
    }
}
