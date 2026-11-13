package com.netflixclone.controller;

import com.netflixclone.dto.AuthRequest;
import com.netflixclone.dto.AuthResponse;
import com.netflixclone.dto.request.EmailRequest;
import com.netflixclone.dto.request.LoginRequest;
import com.netflixclone.dto.request.ResetPasswordRequest;
import com.netflixclone.dto.request.ChangePasswordRequest;
import com.netflixclone.dto.response.MessageResponse;
import com.netflixclone.dto.response.UserResponse;
import com.netflixclone.service.AuthService;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // User signup
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody AuthRequest request) {
        MessageResponse response = authService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    // User login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    // Token validation
    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(user != null);
    }

    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal UserDetails user) {
        return authService.getProfile(user.getUsername());
    }

    @PostMapping("/logout")
    public MessageResponse logout(@AuthenticationPrincipal UserDetails user) {
        return authService.logout(user.getUsername());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request.getEmail()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request.getToken(), request.getNewPassword()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request.getEmail()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(
                user.getUsername(), request.getCurrentPassword(), request.getNewPassword()));
    }
}
