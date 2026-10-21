package com.institute.ims.service;

import com.institute.ims.dto.CreateUserRequest;
import com.institute.ims.dto.UpdateUserRequest;
import com.institute.ims.dto.VerifyBulkRequest;
import com.institute.ims.dto.VerifyBulkResponse;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByRollNumber(String rollNumber) {
        return userRepository.findByRollNumber(rollNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(VerificationStatus.PENDING);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User createUser(CreateUserRequest request) {

        if (userRepository.existsByRollNumber(request.getRollNumber())) {
            throw new RuntimeException("A user with this roll number already exists.");
        }

        Department department = null;

        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(
                    request.getDepartmentId()).orElseThrow(() -> new RuntimeException("Department not found"));
        }

        User user = new User();

        user.setName(request.getName());
        user.setRollNumber(request.getRollNumber());
        user.setPassword(
                passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setDepartment(department);
        user.setStatus(VerificationStatus.VERIFIED);

        return userRepository.save(user);
    }

    public User updateUser(
            Long id,
            UpdateUserRequest request) {

        User user = getUserById(id);

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getDepartmentId() != null) {

            Department department = departmentRepository.findById(
                    request.getDepartmentId()).orElseThrow(
                            () -> new RuntimeException(
                                     "Department not found"));

            user.setDepartment(department);
        }

        return userRepository.save(user);
    }

    /**
     * Verify a single user. Only PENDING users can be verified.
     */
    public User verifyUser(Long id, String requestingUserRollNumber) {
        User user = getUserById(id);

        if (user.getRollNumber().equals(requestingUserRollNumber)) {
            throw new RuntimeException("Users cannot verify themselves.");
        }

        if (user.getStatus() == VerificationStatus.VERIFIED) {
            throw new RuntimeException("User is already verified.");
        }

        user.setStatus(VerificationStatus.VERIFIED);
        return userRepository.save(user);
    }

    /**
     * Bulk verify users by IDs. Skips already verified and self-verification.
     */
    public VerifyBulkResponse verifyUsers(VerifyBulkRequest request, String requestingUserRollNumber) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new RuntimeException("No user IDs provided for verification.");
        }

        List<User> users = userRepository.findByIdIn(request.getUserIds());
        List<Long> verifiedIds = new ArrayList<>();

        for (User user : users) {
            // Skip self-verification
            if (user.getRollNumber().equals(requestingUserRollNumber)) {
                continue;
            }
            // Skip already verified
            if (user.getStatus() == VerificationStatus.VERIFIED) {
                continue;
            }
            user.setStatus(VerificationStatus.VERIFIED);
            verifiedIds.add(user.getId());
        }

        if (!verifiedIds.isEmpty()) {
            userRepository.saveAll(users.stream()
                    .filter(u -> verifiedIds.contains(u.getId()))
                    .toList());
        }

        return new VerifyBulkResponse(
                verifiedIds.size(),
                verifiedIds,
                verifiedIds.isEmpty()
                        ? "No users were verified (all already verified or invalid)."
                        : verifiedIds.size() + " user(s) verified successfully."
        );
    }

    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }

        userRepository.deleteById(id);
    }
}
