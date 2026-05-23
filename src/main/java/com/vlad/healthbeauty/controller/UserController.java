package com.vlad.healthbeauty.controller;

import com.vlad.healthbeauty.model.Role;
import com.vlad.healthbeauty.model.User;
import com.vlad.healthbeauty.repository.RoleRepository;
import com.vlad.healthbeauty.repository.UserRepository;
import com.vlad.healthbeauty.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, AuditLogService auditLogService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
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
    @Operation(summary = "Update user", description = "Access: ROLE_ADMIN only. Can update full name, username, password, and role.")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        StringBuilder changes = new StringBuilder("Updated user " + user.getFullName() + ": ");
        boolean hasChanges = false;

        // Update full name
        if (body.containsKey("fullName") && !body.get("fullName").trim().isEmpty()) {
            String oldFullName = user.getFullName();
            user.setFullName(body.get("fullName").trim());
            changes.append("name changed from '").append(oldFullName).append("' to '").append(user.getFullName()).append("', ");
            hasChanges = true;
        }

        // Update username
        if (body.containsKey("username") && !body.get("username").trim().isEmpty()) {
            String newUsername = body.get("username").trim();
            // Check if username already exists (and it's not the current user's username)
            if (!newUsername.equals(user.getUsername()) && userRepository.findByUsername(newUsername).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            String oldUsername = user.getUsername();
            user.setUsername(newUsername);
            changes.append("username changed from '").append(oldUsername).append("' to '").append(newUsername).append("', ");
            hasChanges = true;
        }

        // Update password (only if provided)
        if (body.containsKey("password") && !body.get("password").trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(body.get("password")));
            changes.append("password changed, ");
            hasChanges = true;
        }

        // Update role
        if (body.containsKey("role") && !body.get("role").trim().isEmpty()) {
            String newRoleName = body.get("role");
            Optional<Role> newRole = roleRepository.findByName(newRoleName);
            if (newRole.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
            }

            String oldRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("UNKNOWN");

            if (!oldRole.equals(newRoleName)) {
                user.setRoles(Set.of(newRole.get()));
                changes.append("role changed from ").append(oldRole).append(" to ").append(newRoleName);
                hasChanges = true;
            }
        }

        if (!hasChanges) {
            return ResponseEntity.badRequest().body(Map.of("error", "No changes provided"));
        }

        userRepository.save(user);

        // Remove trailing comma and space if present
        String changeLog = changes.toString().replaceAll(", $", "");
        
        auditLogService.log(
            "UPDATE",
            "USER",
            user.getId(),
            changeLog
        );

        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
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
