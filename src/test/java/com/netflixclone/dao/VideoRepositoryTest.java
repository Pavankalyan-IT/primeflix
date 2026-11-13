package com.netflixclone.dao;

import com.netflixclone.entity.User;
import com.netflixclone.entity.Video;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class VideoRepositoryTest {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void filtersPublishedVideosAndPersistsUserWatchlist() {
        Video published = new Video();
        published.setTitle("Published sci-fi");
        published.setPublished(true);
        published.setCategories(new ArrayList<>(List.of("Sci-Fi")));
        videoRepository.saveAndFlush(published);

        Video draft = new Video();
        draft.setTitle("Draft sci-fi");
        draft.setPublished(false);
        draft.setCategories(new ArrayList<>(List.of("Sci-Fi")));
        videoRepository.saveAndFlush(draft);

        var page = videoRepository.findDistinctByPublishedTrueAndCategoriesContaining(
                "Sci-Fi", PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("Published sci-fi", page.getContent().get(0).getTitle());
        assertEquals(1, videoRepository.findByPublishedTrue(PageRequest.of(0, 10))
                .getTotalElements());

        User user = new User();
        user.setEmail("watchlist@example.com");
        user.setPassword("encoded-password");
        user.setFullname("Watchlist User");
        user.addToWatchlist(published);
        userRepository.saveAndFlush(user);

        User reloaded = userRepository.findByEmailIgnoreCase("watchlist@example.com")
                .orElseThrow();
        assertTrue(reloaded.getWatchlist().stream()
                .anyMatch(video -> video.getTitle().equals("Published sci-fi")));
    }
}
