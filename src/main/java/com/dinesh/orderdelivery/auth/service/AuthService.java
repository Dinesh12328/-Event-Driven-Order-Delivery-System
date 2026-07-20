package com.dinesh.orderdelivery.auth.service;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.dto.AuthResponse;
import com.dinesh.orderdelivery.auth.dto.LoginRequest;
import com.dinesh.orderdelivery.auth.dto.RegisterRequest;
import com.dinesh.orderdelivery.auth.dto.UserResponse;
import com.dinesh.orderdelivery.auth.mapper.UserMapper;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.common.error.BadRequestException;
import com.dinesh.orderdelivery.common.error.ResourceNotFoundException;
import com.dinesh.orderdelivery.security.JwtService;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            UserMapper userMapper,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email is already registered");
        }
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        AppUser user = userRepository.save(new AppUser(
                request.fullName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                role
        ));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(),
                request.password()
        ));
        AppUser user = findByEmail(request.email());
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return userMapper.toResponse(findByEmail(email));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    private AuthResponse authResponse(AppUser user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.expirationMinutes(), userMapper.toResponse(user));
    }

    private AppUser findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

