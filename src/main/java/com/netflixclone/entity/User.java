package com.netflixclone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.netflixclone.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullname;

@Enumerated(EnumType.STRING)
@Column(nullable = false)
    private Role role = Role.USER;

@Column(nullable = false)
private boolean active = true;


@Column(nullable = false)
private boolean emailVerified =false;

@Column(nullable = false, columnDefinition = "integer default 0")
private int tokenVersion;

@Column(unique = true)
private String verificationToken;

@Column(columnDefinition = "datetime")
private Instant verificationTokenExpiry;

@Column
private String passwordRestToken;
@Column(columnDefinition = "datetime")
private Instant passwordRestTokenExpiry;
@CreationTimestamp
    @Column(nullable = false,updatable = false, columnDefinition = "datetime")
    private Instant createdAt;

@UpdateTimestamp
@Column(nullable = false, columnDefinition = "datetime")
private Instant updatedAt;

@JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Watchlist> watchlistEntries = new ArrayList<>();

public void addToWatchlist(Video video){
    Watchlist entry = new Watchlist();
    entry.setUser(this);
    entry.setVideo(video);
    this.watchlistEntries.add(entry);
}
public void removeFromWatchlist(Video video){
    this.watchlistEntries.removeIf(entry -> entry.getVideo().getId().equals(video.getId()));
}

    public List<Video> getWatchlist() {
        return watchlistEntries.stream()
                .map(Watchlist::getVideo)
                .toList();
    }




}
