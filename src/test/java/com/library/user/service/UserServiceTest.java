package com.library.user.service;

import com.library.auth.security.UserPrincipal;
import com.library.common.exception.ResourceNotFoundException;
import com.library.user.dto.UserResponse;
import com.library.user.model.Role;
import com.library.user.model.User;
import com.library.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Get current user profile returns correct data")
    void shouldReturnCurrentUserProfile() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "user@example.com", "pass", Role.MEMBER, true);
        User user = new User(userId, "user@example.com", "hash", "Standard User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(principal);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Update role modifies role and returns updated response")
    void shouldUpdateRole() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateRole(userId, Role.LIBRARIAN);

        assertThat(response.role()).isEqualTo(Role.LIBRARIAN);
        assertThat(user.getRole()).isEqualTo(Role.LIBRARIAN);
    }

    @Test
    @DisplayName("Update active status toggles account status")
    void shouldUpdateActiveStatus() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateActiveStatus(userId, false);

        assertThat(response.active()).isFalse();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when user does not exist")
    void shouldThrowWhenUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
