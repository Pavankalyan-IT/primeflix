package com.netflixclone.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.netflixclone.dao.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractJwtToken(request);
        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtUtil.validateToken(token)) {
            authenticateRequest(request, token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(HttpServletRequest request, String token) {
        String email = jwtUtil.extractEmail(token);
        String tokenRole = jwtUtil.extractRole(token);
        int tokenVersion = jwtUtil.extractTokenVersion(token);
        Optional<com.netflixclone.entity.User> user =
                userRepository.findByEmailIgnoreCase(email);
        if (user.isEmpty()) {
            return;
        }

        com.netflixclone.entity.User account = user.get();
        if (!account.isActive()
                || !account.isEmailVerified()
                || account.getTokenVersion() != tokenVersion
                || !account.getRole().name().equals(tokenRole)) {
            return;
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(account.getEmail())
                .password("")
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + account.getRole().name())))
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Extract bearer credentials from the Authorization header only.
    private String extractJwtToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

}
