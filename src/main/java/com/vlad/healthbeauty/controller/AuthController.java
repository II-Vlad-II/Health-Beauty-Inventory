package com.vlad.healthbeauty.controller;

import com.vlad.healthbeauty.model.Role;
import com.vlad.healthbeauty.model.User;
import com.vlad.healthbeauty.repository.RoleRepository;
import com.vlad.healthbeauty.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Access: Authenticated. Returns username, full name, and roles.")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        // Get full name from database
        String fullName = userRepository.findByUsername(username)
                .map(User::getFullName)
                .orElse(username);  // Fallback to username if not found

        return ResponseEntity.ok(Map.of(
                "username", username,
                "fullName", fullName,
                "roles", roles
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Access: Public. Assigns specified role (ROLE_ADMIN or ROLE_STAFF).")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Auto hash!
        user.setFullName(request.getFullName());

        // Assign the specified role (default to ROLE_STAFF if not provided)
        String roleName = request.getRole() != null ? request.getRole() : "ROLE_STAFF";
        Optional<Role> userRole = roleRepository.findByName(roleName);
        if (userRole.isPresent()) {
            user.setRoles(Collections.singleton(userRole.get()));
        } else {
            return ResponseEntity.badRequest().body("Invalid role specified");
        }

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // Simple DTO for request
    static class RegisterRequest {
        private String username;
        private String password;
        private String fullName;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}

