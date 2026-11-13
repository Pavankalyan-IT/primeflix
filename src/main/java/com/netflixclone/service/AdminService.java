package com.netflixclone.service;

import com.netflixclone.dao.UserRepository;
import com.netflixclone.dao.VideoRepository;
import com.netflixclone.dao.WatchlistRepository;
import com.netflixclone.dto.request.AdminUserUpdateRequest;
import com.netflixclone.dto.response.AdminDashboardResponse;
import com.netflixclone.dto.response.PageResponse;
import com.netflixclone.dto.response.UserResponse;
import com.netflixclone.dto.response.VideoResponse;
import com.netflixclone.entity.User;
import com.netflixclone.enums.Role;
import com.netflixclone.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final WatchlistRepository watchlistRepository;

    public AdminService(UserRepository userRepository, VideoRepository videoRepository, WatchlistRepository watchlistRepository) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.watchlistRepository = watchlistRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalVideos = videoRepository.count();
        long publishedVideos = videoRepository.countByPublishedTrue();
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);
        return new AdminDashboardResponse(
                totalUsers, activeUsers, totalUsers - activeUsers,
                totalVideos, publishedVideos, totalVideos - publishedVideos,
                videoRepository.sumDuration());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size, String search, Boolean active) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasSearch = search != null && !search.isBlank();
        Page<User> results;
        if (active == null && !hasSearch) {
            results = userRepository.findAll(pageable);
        } else if (active == null) {
            results = userRepository.findByEmailContainingIgnoreCaseOrFullnameContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else if (!hasSearch) {
            results = userRepository.findByActive(active, pageable);
        } else {
            results = userRepository.findByActiveAndEmailContainingIgnoreCaseOrActiveAndFullnameContainingIgnoreCase(
                    active, search.trim(), active, search.trim(), pageable);
        }
        return pageResponse(results.map(UserResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getVideos(
            int page, int size, String search, Boolean published) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasSearch = search != null && !search.isBlank();
        Page<com.netflixclone.entity.Video> results;
        if (published == null && !hasSearch) {
            results = videoRepository.findAll(pageable);
        } else if (published == null) {
            results = videoRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
        } else if (!hasSearch) {
            results = videoRepository.findByPublished(published, pageable);
        } else {
            results = videoRepository.findByPublishedAndTitleContainingIgnoreCase(
                    published, search.trim(), pageable);
        }
        return pageResponse(results.map(VideoResponse::fromEntity));
    }

    @Transactional
    public UserResponse updateUser(long id, AdminUserUpdateRequest request) {
        User user = getUser(id);
        user.setFullname(request.fullName().trim());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserResponse setUserRole(long id, String roleName, String adminEmail) {
        User user = getUser(id);
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("You cannot change your own administrator role");
        }
        final Role role;
        try {
            role = Role.valueOf(roleName.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("The final administrator cannot be demoted");
        }
        if (user.getRole() != role) {
            user.setRole(role);
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserResponse setUserActive(long id, boolean active, String adminEmail) {
        User user = getUser(id);
        if (user.getEmail().equalsIgnoreCase(adminEmail) && !active) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }
        if (!active && user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("The final administrator cannot be deactivated");
        }
        if (user.isActive() != active) {
            user.setActive(active);
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(long id, String adminEmail) {
        User user = getUser(id);
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("The final administrator cannot be deleted");
        }
        watchlistRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    private User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be zero or greater and size must be between 1 and 100");
        }
    }

    private <T> PageResponse<T> pageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(),
                page.getNumber(), page.getSize());
    }
}
