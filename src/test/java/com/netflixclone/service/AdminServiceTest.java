package com.netflixclone.service;

import com.netflixclone.dao.UserRepository;
import com.netflixclone.dao.VideoRepository;
import com.netflixclone.dao.WatchlistRepository;
import com.netflixclone.entity.User;
import com.netflixclone.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class AdminServiceTest {

    private UserRepository userRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        adminService = new AdminService(userRepository, mock(VideoRepository.class), mock(WatchlistRepository.class));
    }

    @Test
    void changingRoleInvalidatesExistingTokens() {
        User user = new User();
        user.setId(4L);
        user.setEmail("member@example.com");
        user.setRole(Role.USER);
        user.setTokenVersion(2);
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        adminService.setUserRole(4L, "ADMIN", "root@example.com");

        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(3, user.getTokenVersion());
        verify(userRepository).save(user);
    }

    @Test
    void finalAdministratorCannotBeDemoted() {
        User user = new User();
        user.setId(7L);
        user.setEmail("last-admin@example.com");
        user.setRole(Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> adminService.setUserRole(7L, "USER", "other-admin@example.com"));
    }

    @Test
    void administratorCannotDeleteOwnAccount() {
        User user = new User();
        user.setId(9L);
        user.setEmail("root@example.com");
        user.setRole(Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> adminService.deleteUser(9L, "root@example.com"));
    }
}
