package com.vlad.healthbeauty.controller;

import com.vlad.healthbeauty.model.Role;
import com.vlad.healthbeauty.model.User;
import com.vlad.healthbeauty.repository.RoleRepository;
import com.vlad.healthbeauty.repository.UserRepository;
import com.vlad.healthbeauty.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Access: ROLE_ADMIN only")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("fullName", user.getFullName());
            
            // Get role name
            String roleName = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");
            userMap.put("role", roleName);
            
            return userMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(userList);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role", description = "Access: ROLE_ADMIN only")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        String newRoleName = body.get("role");
        
        Optional<Role> newRole = roleRepository.findByName(newRoleName);
        if (newRole.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
        }

        String oldRole = user.getRoles().stream()
            .findFirst()
            .map(Role::getName)
            .orElse("UNKNOWN");

        user.setRoles(Set.of(newRole.get()));
        userRepository.save(user);

        auditLogService.log(
            "UPDATE_ROLE",
            "USER",
            user.getId(),
            "Changed role for " + user.getFullName() + " from " + oldRole + " to " + newRoleName
        );

        return ResponseEntity.ok(Map.of("message", "User role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Access: ROLE_ADMIN only")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        String fullName = user.getFullName();
        String username = user.getUsername();

        userRepository.deleteById(id);

        auditLogService.log(
            "DELETE",
            "USER",
            id,
            "Deleted user: " + fullName + " (username: " + username + ")"
        );

        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
