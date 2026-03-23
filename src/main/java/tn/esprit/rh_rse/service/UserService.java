package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.request.ChangePasswordRequest;
import tn.esprit.rh_rse.dto.request.CreateUserRequest;
import tn.esprit.rh_rse.dto.request.UpdateUserRequest;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.entity.enums.Role;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(String id);
    UserResponse updateUser(String id, UpdateUserRequest request);
    void deactivateUser(String id);
    void reactivateUser(String id);
    void changePassword(String id, ChangePasswordRequest request);
    List<UserResponse> getUsersByRole(Role role);
    List<UserResponse> getUsersByDepartement(String departement);
}