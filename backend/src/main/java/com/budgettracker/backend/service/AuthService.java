package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.AuthDTO.*;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(User.Role.USER);
        userRepository.save(user);

        log.info("Registered new user: {}", request.email());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        return buildAuthResponse(user, userDetails);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        log.info("User logged in: {}", request.email());

        return buildAuthResponse(user, userDetails);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtUtil.isRefreshTokenValid(refreshToken, userDetails, user.getRefreshTokenVersion())) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        return buildAuthResponse(user, userDetails);
    }

    /**
     * Invalidates all outstanding refresh tokens for the user by incrementing
     * their refreshTokenVersion. Call this on logout or password change.
     */
    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        user.incrementRefreshTokenVersion();
        userRepository.save(user);
        log.info("User logged out and refresh tokens revoked: {}", userEmail);
    }

    private AuthResponse buildAuthResponse(User user, UserDetails userDetails) {
        return new AuthResponse(
                jwtUtil.generateToken(userDetails),
                jwtUtil.generateRefreshToken(userDetails, user.getRefreshTokenVersion()),
                user.getEmail(),
                user.getName()
        );
    }
}