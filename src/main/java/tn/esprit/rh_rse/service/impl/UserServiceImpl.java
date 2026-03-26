package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.ChangePasswordRequest;
import tn.esprit.rh_rse.dto.request.CreateUserRequest;
import tn.esprit.rh_rse.dto.request.UpdateUserRequest;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.exception.EmailAlreadyExistsException;
import tn.esprit.rh_rse.exception.UserNotFoundException;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "L'email " + request.getEmail() + " est déjà utilisé"
            );
        }

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .telephone(request.getTelephone())
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIF)
                .departement(request.getDepartement())
                .poste(request.getPoste())
                .managerId(request.getManagerId())
                .photoUrl(request.getPhotoUrl())
                .adresse(request.getAdresse())
                .dateEmbauche(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur avec l'ID " + id + " introuvable"
                ));
        return toResponse(user);
    }

    @Override
    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur avec l'ID " + id + " introuvable"
                ));

        if (request.getNom() != null) user.setNom(request.getNom());
        if (request.getPrenom() != null) user.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) user.setTelephone(request.getTelephone());
        if (request.getDepartement() != null) user.setDepartement(request.getDepartement());
        if (request.getPoste() != null) user.setPoste(request.getPoste());
        if (request.getManagerId() != null) user.setManagerId(request.getManagerId());
        if (request.getPhotoUrl() != null) user.setPhotoUrl(request.getPhotoUrl());
        if (request.getAdresse() != null) user.setAdresse(request.getAdresse());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Override
    public void deactivateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur avec l'ID " + id + " introuvable"
                ));
        user.setStatus(UserStatus.INACTIF);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void reactivateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur avec l'ID " + id + " introuvable"
                ));
        user.setStatus(UserStatus.ACTIF);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void changePassword(String id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur avec l'ID " + id + " introuvable"
                ));

        if (!passwordEncoder.matches(request.getAncienPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNouveauPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByDepartement(String departement) {
        return userRepository.findByDepartement(departement)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .telephone(user.getTelephone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .departement(user.getDepartement())
                .poste(user.getPoste())
                .managerId(user.getManagerId())
                .photoUrl(user.getPhotoUrl())
                .adresse(user.getAdresse())
                .dateEmbauche(user.getDateEmbauche())
                .createdAt(user.getCreatedAt())
                .build();
    }
    @Override
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}