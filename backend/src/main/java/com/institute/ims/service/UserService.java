package com.institute.ims.service;

import com.institute.ims.dto.CreateUserRequest;
import com.institute.ims.dto.UpdateUserRequest;
import com.institute.ims.dto.UpdateProfileRequest;
import com.institute.ims.dto.VerifyBulkRequest;
import com.institute.ims.dto.VerifyBulkResponse;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            SubjectRepository subjectRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
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

    public List<User> getUsersForRequestingUser(Role role, String requestingUserRollNumber) {
        User requester = getUserByRollNumber(requestingUserRollNumber);
        List<User> users = role == null ? userRepository.findAll() : userRepository.findByRole(role);
        if (requester.getRole() != Role.HOD) {
            return users;
        }
        return filterStudentsToHodDepartments(users, requester);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(VerificationStatus.PENDING);
    }

    public List<User> getPendingUsers(String requestingUserRollNumber) {
        User requester = getUserByRollNumber(requestingUserRollNumber);
        List<User> pendingUsers = getPendingUsers();
        if (requester.getRole() != Role.HOD) {
            return pendingUsers;
        }
        return filterStudentsToHodDepartments(pendingUsers, requester);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserById(Long id, String requestingUserRollNumber) {
        User requester = getUserByRollNumber(requestingUserRollNumber);
        User user = getUserById(id);
        ensureHodStudentScope(requester, user);
        return user;
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

    @Transactional
    public User updateOwnProfile(String rollNumber, UpdateProfileRequest request) {
        User user = getUserByRollNumber(rollNumber);
        user.setName(request.getName().trim());

        String email = request.getEmail() == null ? null : request.getEmail().trim();
        if (email != null && !email.isEmpty()
                && userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new RuntimeException("Email already exists");
        }
        user.setEmail(email == null || email.isEmpty() ? null : email);

        boolean hasAcademicFields = request.getYear() != null
                || request.getSemester() != null
                || request.getOptionalSubjectCodes() != null;
        if (user.getRole() != Role.STUDENT) {
            if (hasAcademicFields) {
                throw new AccessDeniedException("Only students can update academic details.");
            }
            return userRepository.save(user);
        }

        if (request.getYear() == null && request.getSemester() == null) {
            if (request.getOptionalSubjectCodes() != null
                    && !request.getOptionalSubjectCodes().isEmpty()) {
                throw new RuntimeException("Select an academic year and semester before choosing subjects.");
            }
            return userRepository.save(user);
        }

        if (request.getYear() == null || request.getSemester() == null) {
            throw new RuntimeException("Academic year and semester must both be provided.");
        }
        if (request.getYear() < 1 || request.getYear() > 4
                || request.getSemester() < 1 || request.getSemester() > 8
                || request.getYear() != (request.getSemester() + 1) / 2) {
            throw new RuntimeException("Select a valid academic year and matching semester.");
        }
        if (user.getDepartment() == null) {
            throw new RuntimeException("A department is required before selecting subjects.");
        }

        List<String> optionalSubjectCodes = request.getOptionalSubjectCodes() == null
                ? List.of()
                : request.getOptionalSubjectCodes();
        Set<String> requestedCodes = new HashSet<>(optionalSubjectCodes);
        if (requestedCodes.size() != optionalSubjectCodes.size()) {
            throw new RuntimeException("Duplicate subject choices are not allowed.");
        }

        List<Subject> semesterSubjects = subjectRepository.findByDepartmentIdAndSemester(
                user.getDepartment().getId(), request.getSemester());
        Set<String> availableOptionalCodes = semesterSubjects.stream()
                .filter(subject -> subject.getType() == SubjectType.OPTIONAL)
                .map(Subject::getCode)
                .collect(Collectors.toSet());
        if (!availableOptionalCodes.containsAll(requestedCodes)) {
            throw new RuntimeException("Choose only optional subjects for your department and semester.");
        }

        Set<Subject> selectedSubjects = new HashSet<>();
        for (Subject subject : semesterSubjects) {
            if (subject.getType() == SubjectType.COMPULSORY
                    || requestedCodes.contains(subject.getCode())) {
                selectedSubjects.add(subject);
            }
        }
        user.setYear(request.getYear());
        user.setSemester(request.getSemester());
        user.setSelectedSubjects(selectedSubjects);
        return userRepository.save(user);
    }

    /**
     * Verify a single user. Only PENDING users can be verified.
     */
    public User verifyUser(Long id, String requestingUserRollNumber) {
        User requester = getUserByRollNumber(requestingUserRollNumber);
        User user = getUserById(id);

        ensureHodStudentScope(requester, user);

        if (user.getRollNumber().equals(requestingUserRollNumber)) {
            throw new RuntimeException("Users cannot verify themselves.");
        }

        if (user.getStatus() == VerificationStatus.VERIFIED) {
            throw new RuntimeException("User is already verified.");
        }

        user.setStatus(VerificationStatus.VERIFIED);
        return userRepository.save(user);
    }

    public void rejectUser(Long id, String requestingUserRollNumber) {
        User requester = getUserByRollNumber(requestingUserRollNumber);
        User user = getUserById(id);
        ensureHodStudentScope(requester, user);

        if (user.getStatus() != VerificationStatus.PENDING) {
            throw new RuntimeException("Only pending registrations can be rejected.");
        }
        if (user.getRollNumber().equals(requestingUserRollNumber)) {
            throw new RuntimeException("Users cannot reject their own registration.");
        }

        userRepository.delete(user);
    }

    @Transactional
    public int rejectUsers(VerifyBulkRequest request, String requestingUserRollNumber) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new RuntimeException("No user IDs provided for rejection.");
        }

        User requester = getUserByRollNumber(requestingUserRollNumber);
        List<Long> userIds = request.getUserIds().stream().distinct().toList();
        List<User> users = userRepository.findByIdIn(userIds);
        if (users.size() != userIds.size()) {
            throw new RuntimeException("One or more selected users could not be found.");
        }

        for (User user : users) {
            ensureHodStudentScope(requester, user);
            if (user.getStatus() != VerificationStatus.PENDING) {
                throw new RuntimeException("Only pending registrations can be rejected.");
            }
            if (user.getRollNumber().equals(requestingUserRollNumber)) {
                throw new RuntimeException("Users cannot reject their own registration.");
            }
        }

        userRepository.deleteAll(users);
        return users.size();
    }

    /**
     * Bulk verify users by IDs. Skips already verified and self-verification.
     */
    public VerifyBulkResponse verifyUsers(VerifyBulkRequest request, String requestingUserRollNumber) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new RuntimeException("No user IDs provided for verification.");
        }

        User requester = getUserByRollNumber(requestingUserRollNumber);
        List<User> users = userRepository.findByIdIn(request.getUserIds());
        List<Long> verifiedIds = new ArrayList<>();
        Set<Long> departmentIds = requester.getRole() == Role.HOD
                ? getHodDepartmentIds(requester)
                : Set.of();

        for (User user : users) {
            if (requester.getRole() == Role.HOD && user.getRole() == Role.STUDENT
                    && (user.getDepartment() == null
                    || !departmentIds.contains(user.getDepartment().getId()))) {
                continue;
            }
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

    private void ensureHodStudentScope(User requester, User target) {
        if (requester.getRole() == Role.HOD && target.getRole() == Role.STUDENT
                && (target.getDepartment() == null
                || !getHodDepartmentIds(requester).contains(target.getDepartment().getId()))) {
            throw new AccessDeniedException("You may only manage students in your assigned department.");
        }
    }

    private List<User> filterStudentsToHodDepartments(List<User> users, User hod) {
        Set<Long> departmentIds = getHodDepartmentIds(hod);
        return users.stream()
                .filter(user -> user.getRole() != Role.STUDENT
                        || (user.getDepartment() != null && departmentIds.contains(user.getDepartment().getId())))
                .toList();
    }

    private Set<Long> getHodDepartmentIds(User hod) {
        return departmentRepository.findByHodId(hod.getId()).stream()
                .map(Department::getId)
                .collect(Collectors.toSet());
    }
}
