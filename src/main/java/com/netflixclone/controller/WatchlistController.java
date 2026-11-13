package com.netflixclone.controller;

import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<VideoResponse> getWatchlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        return watchlistService.getWatchlist(userDetails.getUsername());
    }

    @PostMapping("/{videoId}")
    public VideoResponse addVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable long videoId) {
        return watchlistService.addVideo(userDetails.getUsername(), videoId);
    }

    @DeleteMapping("/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable long videoId) {
        watchlistService.removeVideo(userDetails.getUsername(), videoId);
    }
}
