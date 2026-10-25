package com.institute.ims.controller;

import com.institute.ims.dto.UpdateUserRequest;
import com.institute.ims.dto.VerifyBulkRequest;
import com.institute.ims.dto.VerifyBulkResponse;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getUsers(
            @RequestParam(required = false) Role role,
            Authentication authentication) {
        return ResponseEntity.ok(
                userService.getUsersForRequestingUser(role, authentication.getName())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.getUserById(id, authentication.getName())
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(
                userService.updateUser(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.ok("User deleted successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.getUserByRollNumber(
                        authentication.getName()
                )
        );
    }

    // ───────── Admin verification endpoints ─────────

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public ResponseEntity<List<User>> getPendingUsers(Authentication authentication) {
        return ResponseEntity.ok(userService.getPendingUsers(authentication.getName()));
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public ResponseEntity<User> verifyUser(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.verifyUser(id, authentication.getName())
        );
    }

    @PostMapping("/verify-bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public ResponseEntity<VerifyBulkResponse> verifyBulk(
            @Valid @RequestBody VerifyBulkRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.verifyUsers(request, authentication.getName())
        );
    }
}
