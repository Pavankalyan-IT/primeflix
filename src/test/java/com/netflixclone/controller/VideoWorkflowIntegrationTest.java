package com.netflixclone.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflixclone.dao.UserRepository;
import com.netflixclone.dao.VideoRepository;
import com.netflixclone.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VideoWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        videoRepository.deleteAll();

        User user = new User();
        user.setEmail("watcher@example.com");
        user.setPassword("encoded-password");
        user.setFullname("Watcher");
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
        videoRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "watcher@example.com", roles = "ADMIN")
    void createsUploadsPublishesSavesToWatchlistAndDeletesVideo() throws Exception {
        String created = mockMvc.perform(post("/api/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Integration test movie","description":"test",
                                 "year":2025,"rating":"PG-13","duration":90,
                                 "published":false,"categories":["Drama"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode createdJson = objectMapper.readTree(created);
        long videoId = createdJson.get("id").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file", "movie.mp4", "video/mp4",
                new byte[]{0, 0, 0, 8, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});
        String uploaded = mockMvc.perform(multipart("/api/videos/{id}/video", videoId).file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String mediaId = objectMapper.readTree(uploaded).get("src").asText()
                .substring(objectMapper.readTree(uploaded).get("src").asText().lastIndexOf('/') + 1);
        mockMvc.perform(get("/api/files/video/{id}", mediaId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/media/video/{id}", mediaId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Integration test movie","description":"test",
                                 "year":2025,"rating":"PG-13","duration":90,
                                 "published":true,"categories":["Drama"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.src").isNotEmpty());
        mockMvc.perform(get("/api/files/video/{id}", mediaId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/videos").param("category", "Drama"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/videos").param("search", "INTEGRATION TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/videos").param("search", "not a title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(post("/api/watchlist/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isInWatchlist").value(true));

        mockMvc.perform(delete("/api/videos/{id}", videoId))
                .andExpect(status().isNoContent());

        String watchlist = mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, objectMapper.readTree(watchlist).size());

        mockMvc.perform(get("/api/videos/{id}", videoId))
                .andExpect(status().isNotFound());
    }
}
