package tn.esprit.rh_rse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.rh_rse.dto.request.CreateUserRequest;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;
import tn.esprit.rh_rse.exception.EmailAlreadyExistsException;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.impl.UserServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private CreateUserRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("u123")
                .nom("Doe")
                .prenom("John")
                .email("john.doe@test.com")
                .password("encodedPassword")
                .role(Role.CANDIDAT)
                .status(UserStatus.ACTIF)
                .build();

        createRequest = new CreateUserRequest();
        createRequest.setNom("Doe");
        createRequest.setPrenom("John");
        createRequest.setEmail("john.doe@test.com");
        createRequest.setPassword("rawPassword");
        createRequest.setRole(Role.CANDIDAT);
    }

    @Test
    void createUser_ShouldReturnUserResponse_WhenEmailIsUnique() {
        when(userRepository.existsByEmail("john.doe@test.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(createRequest);

        assertNotNull(response);
        assertEquals("john.doe@test.com", response.getEmail());
        assertEquals(Role.CANDIDAT, response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail("john.doe@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(createRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deactivateUser_ShouldChangeStatusToInactif() {
        when(userRepository.findById("u123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deactivateUser("u123");

        assertEquals(UserStatus.INACTIF, user.getStatus());
        verify(userRepository, times(1)).save(user);
    }
}
