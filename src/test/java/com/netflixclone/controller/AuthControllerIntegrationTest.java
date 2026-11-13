package com.netflixclone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflixclone.dao.UserRepository;
import com.netflixclone.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender mailSender;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
        reset(mailSender);
    }

    @Test
    void signupVerificationAndLoginWorkThroughHttpEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"watcher@example.com","password":"password123","fullName":"Watcher"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Registration successful. Check your email to verify your account."));

        ArgumentCaptor<SimpleMailMessage> sentMail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sentMail.capture());
        assertNotNull(userRepository.findByEmailIgnoreCase("watcher@example.com").orElse(null));
        String verificationToken = tokenFrom(sentMail.getValue());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"watcher@example.com","password":"password123"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/verify-email")
                        .param("token", verificationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified. You can now sign in."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"watcher@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"watcher@example.com"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<SimpleMailMessage> resetMail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(resetMail.capture());
        String resetToken = tokenFrom(resetMail.getAllValues().get(1));
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("token", resetToken, "newPassword", "new-password-456"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"watcher@example.com","password":"new-password-456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertTrue(userRepository.findByEmailIgnoreCase("watcher@example.com")
                .orElseThrow().isEmailVerified());
    }

    @Test
    void jwtAuthenticatesProtectedRoutesAndMemberCannotUseAdminRoutes() throws Exception {
        User user = new User();
        user.setEmail("member@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullname("Member");
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String jwt = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/profile")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.fullname").value("Member"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/videos")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Forbidden video","published":false}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"new-password-456"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"new-password-456","newPassword":"another-password-789"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"another-password-789"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanCreateOnlyStandardUnverifiedMemberAccounts() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"created@example.com","password":"initial-password-123",
                                 "fullName":"Created Member"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(
                        "Registration successful. Check your email to verify your account."));

        User created = userRepository.findByEmailIgnoreCase("created@example.com").orElseThrow();
        assertTrue(created.getRole() == com.netflixclone.enums.Role.USER);
        assertTrue(!created.isEmailVerified());
    }

    @Test
    void logoutInvalidatesTheExistingJwt() throws Exception {
        User user = new User();
        user.setEmail("logout@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullname("Logout Member");
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);
        String login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"logout@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String jwt = objectMapper.readTree(login).get("token").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/profile")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    private String tokenFrom(SimpleMailMessage message) {
        Matcher matcher = Pattern.compile("token=([^\\s]+)").matcher(message.getText());
        if (!matcher.find()) {
            throw new AssertionError("Email did not contain an action token");
        }
        return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
    }
}
