package com.library.auth.service;

import com.library.auth.dto.AuthResponse;
import com.library.auth.dto.LoginRequest;
import com.library.auth.dto.RegisterRequest;
import com.library.auth.security.JwtService;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
import com.library.user.model.Role;
import com.library.user.model.User;
import com.library.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Register creates a new MEMBER and returns access token")
    void shouldRegisterNewMember() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", "Alice Smith");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_hash");

        User savedUser = new User(UUID.randomUUID(), "alice@example.com", "encoded_hash", "Alice Smith", Role.MEMBER, true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("jwt_token_123");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt_token_123");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.user().role()).isEqualTo(Role.MEMBER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register throws ConflictException if email is already taken")
    void shouldThrowConflictWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "Existing User");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Login succeeds with valid credentials")
    void shouldLoginWithValidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        User user = new User(UUID.randomUUID(), "alice@example.com", "encoded_hash", "Alice Smith", Role.MEMBER, true);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt_token_valid");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt_token_valid");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Login throws BadCredentialsException on password mismatch")
    void shouldThrowOnInvalidPassword() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrongpass");
        User user = new User(UUID.randomUUID(), "alice@example.com", "encoded_hash", "Alice Smith", Role.MEMBER, true);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("wrongpass"), eq("encoded_hash"))).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Login throws BusinessRuleException when user is inactive")
    void shouldThrowWhenUserIsDeactivated() {
        LoginRequest request = new LoginRequest("inactive@example.com", "password123");
        User inactiveUser = new User(UUID.randomUUID(), "inactive@example.com", "encoded_hash", "Inactive User", Role.MEMBER, false);

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deactivated");
    }
}
