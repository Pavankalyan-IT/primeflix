package com.netflixclone.service;



import com.netflixclone.dto.AuthRequest;
import com.netflixclone.dto.AuthResponse;
import com.netflixclone.dto.request.LoginRequest;
import com.netflixclone.dto.response.MessageResponse;
import com.netflixclone.dto.response.UserResponse;
import com.netflixclone.dao.UserRepository;
import com.netflixclone.entity.User;
import com.netflixclone.enums.Role;
import com.netflixclone.exception.BadCredentialsException;
import com.netflixclone.exception.AccountDeactivatedException;
import com.netflixclone.exception.EmailNotVerifiedException;
import com.netflixclone.exception.EmailAlreadyExistsException;
import com.netflixclone.exception.InvalidCredentialsException;
import com.netflixclone.exception.InvalidTokenException;
import com.netflixclone.exception.EmailSendingException;
import com.netflixclone.exception.ResourceNotFoundException;
import com.netflixclone.service.EmailService;
import com.netflixclone.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final long VERIFICATION_TOKEN_MINUTES = 24 * 60;
    private static final long RESET_TOKEN_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    private void validatePassword(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException("Password must be no more than 72 UTF-8 bytes");
        }
    }

    @Transactional
    public MessageResponse registerUser(AuthRequest request) {
        validatePassword(request.getPassword());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (request.getFullName() == null || request.getFullName().isBlank()
                || request.getFullName().length() > 255) {
            throw new InvalidCredentialsException("Full name is required and must be at most 255 characters");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullname(request.getFullName().trim());
        user.setRole(Role.USER);
        String rawToken = newToken();
        user.setVerificationToken(hashToken(rawToken));
        user.setVerificationTokenExpiry(Instant.now().plus(VERIFICATION_TOKEN_MINUTES, ChronoUnit.MINUTES));
        userRepository.save(user);
        try {
            emailService.sendVerificationEmail(email, rawToken);
        } catch (EmailSendingException exception) {
            log.warn("Verification email could not be sent for a newly registered account");
            return new MessageResponse(
                    "Account created, but the verification email could not be sent. Check SMTP settings, then use Resend verification email.");
        }
        return new MessageResponse("Registration successful. Check your email to verify your account.");
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticateUser(LoginRequest request) {
        validatePassword(request.getPassword());
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new AccountDeactivatedException("This account is inactive");
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Verify your email before signing in");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name(), user.getTokenVersion());
        return new AuthResponse(token, "Login successful");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(hashToken(token))
                .orElseThrow(() -> new InvalidTokenException("Verification link is invalid or expired"));
        if (user.getVerificationTokenExpiry() == null
                || !user.getVerificationTokenExpiry().isAfter(Instant.now())) {
            throw new InvalidTokenException("Verification link is invalid or expired");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Email verified. You can now sign in.");
    }

    @Transactional
    public MessageResponse requestPasswordReset(String emailAddress) {
        if (emailAddress != null && !emailAddress.isBlank()) {
            userRepository.findByEmailIgnoreCase(emailAddress.trim())
                    .filter(User::isActive)
                    .ifPresent(user -> {
                        String rawToken = newToken();
                        user.setPasswordRestToken(hashToken(rawToken));
                        user.setPasswordRestTokenExpiry(
                                Instant.now().plus(RESET_TOKEN_MINUTES, ChronoUnit.MINUTES));
                        userRepository.save(user);
                        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
                    });
        }
        return new MessageResponse("If an active account exists for that email, password reset instructions have been sent.");
    }

    @Transactional
    public MessageResponse resendVerification(String emailAddress) {
        if (emailAddress != null && !emailAddress.isBlank()) {
            userRepository.findByEmailIgnoreCase(emailAddress.trim())
                    .filter(user -> !user.isEmailVerified() && user.isActive())
                    .ifPresent(user -> {
                        String rawToken = newToken();
                        user.setVerificationToken(hashToken(rawToken));
                        user.setVerificationTokenExpiry(
                                Instant.now().plus(VERIFICATION_TOKEN_MINUTES, ChronoUnit.MINUTES));
                        userRepository.save(user);
                        emailService.sendVerificationEmail(user.getEmail(), rawToken);
                    });
        }
        return new MessageResponse("If an unverified account exists for that email, a verification email has been sent.");
    }

    @Transactional
    public MessageResponse changePassword(
            String email, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid account"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPasswordRestToken(null);
        user.setPasswordRestTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Password updated successfully.");
    }

    @Transactional
    public MessageResponse resetPassword(String token, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findByPasswordRestToken(hashToken(token))
                .orElseThrow(() -> new InvalidTokenException("Password reset link is invalid or expired"));
        if (user.getPasswordRestTokenExpiry() == null
                || !user.getPasswordRestTokenExpiry().isAfter(Instant.now())) {
            throw new InvalidTokenException("Password reset link is invalid or expired");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPasswordRestToken(null);
        user.setPasswordRestTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Password updated successfully.");
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public MessageResponse logout(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        return new MessageResponse("Signed out successfully.");
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is invalid or expired");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
