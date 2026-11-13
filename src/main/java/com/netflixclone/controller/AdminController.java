package com.netflixclone.controller;

import com.netflixclone.dto.AuthRequest;
import com.netflixclone.dto.response.MessageResponse;
import com.netflixclone.dto.request.AdminUserUpdateRequest;
import com.netflixclone.dto.response.AdminDashboardResponse;
import com.netflixclone.dto.response.PageResponse;
import com.netflixclone.dto.response.UserResponse;
import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.service.AdminService;
import com.netflixclone.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return adminService.getUsers(page, size, search, active);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse createUser(@Valid @RequestBody AuthRequest request) {
        return authService.registerUser(request);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(
            @PathVariable long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        return adminService.updateUser(id, request);
    }

    @PutMapping("/users/{id}/role")
    public UserResponse setRole(
            @PathVariable long id,
            @RequestParam String role,
            @AuthenticationPrincipal UserDetails admin) {
        return adminService.setUserRole(id, role, admin.getUsername());
    }

    @PutMapping("/users/{id}/status")
    public UserResponse setStatus(
            @PathVariable long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails admin) {
        return adminService.setUserActive(id, active, admin.getUsername());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable long id, @AuthenticationPrincipal UserDetails admin) {
        adminService.deleteUser(id, admin.getUsername());
    }

    @GetMapping("/videos")
    public PageResponse<VideoResponse> videos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean published) {
        return adminService.getVideos(page, size, search, published);
    }
}
