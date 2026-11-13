package com.netflixclone.dao;

import com.netflixclone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.netflixclone.enums.Role;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordRestToken(String passwordRestToken);

    Page<User> findByEmailContainingIgnoreCaseOrFullnameContainingIgnoreCase(
            String email, String fullname, Pageable pageable);

    Page<User> findByActive(boolean active, Pageable pageable);

    Page<User> findByActiveAndEmailContainingIgnoreCaseOrActiveAndFullnameContainingIgnoreCase(
            boolean firstActive, String email, boolean secondActive, String fullname, Pageable pageable);

    long countByActive(boolean active);

    long countByRole(Role role);

    void deleteAllByWatchlistEntriesUserId(long userId);
}
