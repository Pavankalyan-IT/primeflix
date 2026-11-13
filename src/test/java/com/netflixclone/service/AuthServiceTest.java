package com.netflixclone.service;

import com.netflixclone.dao.UserRepository;
import com.netflixclone.dto.AuthRequest;
import com.netflixclone.dto.request.LoginRequest;
import com.netflixclone.entity.User;
import com.netflixclone.exception.EmailNotVerifiedException;
import com.netflixclone.exception.EmailSendingException;
import com.netflixclone.dto.response.MessageResponse;
import com.netflixclone.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private EmailService emailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        emailService = mock(EmailService.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, emailService);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    }

    @Test
    void registrationRequiresVerificationBeforeLoginAndTokenCanOnlyBeUsedOnce() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        AuthRequest signup = new AuthRequest();
        signup.setEmail(" Person@Example.com ");
        signup.setPassword("password123");
        signup.setFullName("Person");

        authService.registerUser(signup);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        User user = savedUser.getValue();
        assertEquals("person@example.com", user.getEmail());
        assertNotEquals("encoded-password", user.getVerificationToken());
        assertEquals(false, user.isEmailVerified());
        assertEquals(true, user.getVerificationTokenExpiry().isAfter(Instant.now()));

        ArgumentCaptor<String> sentToken = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(
                org.mockito.ArgumentMatchers.eq("person@example.com"), sentToken.capture());

        LoginRequest login = new LoginRequest();
        login.setEmail("person@example.com");
        login.setPassword("password123");
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        assertThrows(EmailNotVerifiedException.class, () -> authService.authenticateUser(login));

        when(userRepository.findByVerificationToken(user.getVerificationToken())).thenReturn(Optional.of(user));
        authService.verifyEmail(sentToken.getValue());
        assertEquals(true, user.isEmailVerified());
        assertEquals(null, user.getVerificationToken());

        when(jwtUtil.generateToken("person@example.com", "USER", 0)).thenReturn("signed-jwt");
        assertEquals("signed-jwt", authService.authenticateUser(login).getToken());
        assertThrows(RuntimeException.class, () -> authService.verifyEmail(sentToken.getValue()));
    }

    @Test
    void registrationKeepsAccountAndExplainsWhenVerificationEmailFails() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        doThrow(new EmailSendingException("Email could not be sent", new RuntimeException()))
                .when(emailService).sendVerificationEmail(
                        org.mockito.ArgumentMatchers.eq("person@example.com"), any(String.class));
        AuthRequest signup = new AuthRequest();
        signup.setEmail("person@example.com");
        signup.setPassword("password123");
        signup.setFullName("Person");

        MessageResponse response = authService.registerUser(signup);

        verify(userRepository).save(any(User.class));
        assertTrue(response.getMessage().contains("Account created"));
        assertTrue(response.getMessage().contains("Resend verification email"));
    }
}
