package com.netflixclone.config;

import com.netflixclone.dao.UserRepository;
import com.netflixclone.entity.User;
import com.netflixclone.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Component
@Profile("!test")
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean emailConfigured = adminEmail != null && !adminEmail.isBlank();
        boolean passwordConfigured = adminPassword != null && !adminPassword.isBlank();
        if (!emailConfigured && !passwordConfigured) {
            return;
        }
        if (!emailConfigured || !passwordConfigured || adminPassword.length() < 12
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Configure ADMIN_EMAIL and an ADMIN_PASSWORD of 12 to 72 UTF-8 bytes");
        }

        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            User newAdmin = new User();
            newAdmin.setEmail(normalizedEmail);
            newAdmin.setFullname("Administrator");
            newAdmin.setPassword(passwordEncoder.encode(adminPassword));
            return newAdmin;
        });
        if (user.getRole() == Role.ADMIN && user.isActive() && user.isEmailVerified()) {
            return;
        }
        user.setRole(Role.ADMIN);
        user.setActive(true);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }
}
