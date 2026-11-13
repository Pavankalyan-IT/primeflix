package com.netflixclone.dto.response;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long deactivatedUsers,
        long totalVideos,
        long publishedVideos,
        long draftVideos,
        long totalDurationMinutes) {
}
