package com.library.auth.service;

import com.library.auth.dto.AuthResponse;
import com.library.auth.dto.LoginRequest;
import com.library.auth.dto.RegisterRequest;
import com.library.auth.security.JwtService;
import com.library.auth.security.UserPrincipal;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
import com.library.user.dto.UserResponse;
import com.library.user.model.Role;
import com.library.user.model.User;
import com.library.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new ConflictException("Email is already registered: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.MEMBER);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.from(savedUser);
        String token = jwtService.generateToken(principal);

        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BusinessRuleException("Your account is deactivated. Please contact library support.");
        }

        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtService.generateToken(principal);

        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(user));
    }
}
