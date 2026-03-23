package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.ChangePasswordRequest;
import tn.esprit.rh_rse.dto.request.CreateUserRequest;
import tn.esprit.rh_rse.dto.request.UpdateUserRequest;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id,
                                                   @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable String id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable String id) {
        userService.reactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String id,
                                               @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/department/{departement}")
    public ResponseEntity<List<UserResponse>> getUsersByDepartement(
            @PathVariable String departement) {
        return ResponseEntity.ok(userService.getUsersByDepartement(departement));
    }
}