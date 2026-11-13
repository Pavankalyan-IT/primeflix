package com.netflixclone.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "video_category")
@Getter
@Setter
public class VideoCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "category", nullable = false, length = 100)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoCategory that = (VideoCategory) o;
        return video != null && name != null
                && video.getId().equals(that.video.getId())
                && name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return 31 * video.getId().hashCode() + name.toLowerCase(java.util.Locale.ROOT).hashCode();
    }
}
