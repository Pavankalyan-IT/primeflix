package com.netflixclone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

import java.time.Instant;

@Entity
@Table(name="videos")
@Getter
@Setter
public class Video {
    @Id
    @GeneratedValue(strategy  = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;


    @Column(length = 40000)
    private String description;


    @Column(name = "release_year")
    private Integer year;
    private String rating;
    private Integer duration;

    @Column(name="src")
    @JsonIgnore
    private String srcUuid;

    @Column(name ="poster")
    @JsonIgnore
    private String posterUuid;


    @Column(nullable = false)
    private boolean published =false;

    @OneToMany(mappedBy = "video", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoCategory> categoryEntities = new ArrayList<>();


@CreationTimestamp
    @Column(nullable = false,updatable = false, columnDefinition = "datetime")
    private Instant createdAt;



@UpdateTimestamp
    @Column(nullable = false, columnDefinition = "datetime")
    private Instant updatedAt;
@Transient
    @JsonProperty("isInWatchlist")
    private Boolean isInWatchList;

    public List<String> getCategories() {
        return categoryEntities.stream()
                .map(VideoCategory::getName)
                .toList();
    }

    public void setCategories(List<String> categoryNames) {
        this.categoryEntities.clear();
        if (categoryNames != null) {
            for (String name : categoryNames) {
                VideoCategory vc = new VideoCategory();
                vc.setVideo(this);
                vc.setName(name);
                this.categoryEntities.add(vc);
            }
        }
    }

@JsonProperty("src")
    public String getSrc(){
    if(srcUuid != null && !srcUuid.isEmpty()){
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
                return baseUrl + "/api/files/video/"+srcUuid;
    }
    return null;

}
@JsonProperty("poster")
    public String getPoster(){
    if (posterUuid != null && !posterUuid.isEmpty()) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        return baseUrl+ "/api/files/image/"+posterUuid;

    }
    return null;
}




}
