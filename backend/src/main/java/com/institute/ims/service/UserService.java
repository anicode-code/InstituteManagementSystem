package com.institute.ims.service;

import com.institute.ims.dto.CreateUserRequest;
import com.institute.ims.dto.UpdateUserRequest;
import com.institute.ims.dto.UpdateProfileRequest;
import com.institute.ims.dto.VerifyBulkRequest;
import com.institute.ims.dto.VerifyBulkResponse;
import com.institute.ims.model.Department;
import com.institute.ims.model.FacultyProfileAudit;
import com.institute.ims.model.FacultyProfileOption;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.FacultyProfileAuditRepository;
import com.institute.ims.repository.FacultyProfileOptionRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FacultyProfileOptionRepository facultyProfileOptionRepository;
    private final FacultyProfileAuditRepository facultyProfileAuditRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            FacultyProfileOptionRepository facultyProfileOptionRepository,
            FacultyProfileAuditRepository facultyProfileAuditRepository,
            SubjectRepository subjectRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.facultyProfileOptionRepository = facultyProfileOptionRepository;
        this.facultyProfileAuditRepository = facultyProfileAuditRepository;
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
        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email != null && !email.isEmpty() && userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email already exists");
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
        user.setEmail(email == null || email.isEmpty() ? null : email);
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
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.isEmpty()
                    && userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email.isEmpty() ? null : email);
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
        boolean hasFacultyFields = request.getPhone() != null
                || request.isDateOfBirthProvided()
                || request.getGender() != null
                || request.getResidentialAddress() != null
                || request.isJoiningDateProvided()
                || request.getEmploymentStatus() != null
                || request.getDesignation() != null
                || request.getAdministrativeRole() != null
                || request.getQualification() != null
                || request.getSpecialization() != null
                || request.isYearsOfExperienceProvided()
                || request.getEmploymentType() != null
                || request.getDepartmentIds() != null;
        if (user.getRole() != Role.FACULTY && hasFacultyFields) {
            throw new AccessDeniedException("Only faculty can update faculty profile details.");
        }

        Map<Long, Department> facultyDepartments = null;
        Map<String, String> previousFacultyProfile = null;
        if (user.getRole() == Role.FACULTY) {
            facultyDepartments = validateFacultyProfileRequest(request);
            previousFacultyProfile = facultyProfileValues(user);
        }

        user.setName(request.getName().trim());

        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email != null && !email.isEmpty()
                && userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new RuntimeException("Email already exists");
        }
        if (email != null && !email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RuntimeException("Email must be a valid email address.");
        }
        user.setEmail(email == null || email.isEmpty() ? null : email);

        boolean hasAcademicFields = request.getYear() != null
                || request.getSemester() != null
                || request.getOptionalSubjectCodes() != null;
        if (user.getRole() != Role.STUDENT) {
            if (hasAcademicFields) {
                throw new AccessDeniedException("Only students can update academic details.");
            }
            if (user.getRole() == Role.FACULTY) {
                updateFacultyProfileFields(user, request, facultyDepartments);
            }
            User savedUser = userRepository.save(user);
            if (previousFacultyProfile != null) {
                recordFacultyProfileChanges(
                        savedUser,
                        rollNumber,
                        previousFacultyProfile,
                        facultyProfileValues(savedUser));
            }
            return savedUser;
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

    private void updateFacultyProfileFields(
            User user,
            UpdateProfileRequest request,
            Map<Long, Department> departmentsById) {
        if (request.getPhone() != null) {
            user.setPhone(normalizeOptionalText(request.getPhone()));
        }
        if (request.isDateOfBirthProvided()) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(normalizeOptionalText(request.getGender()));
        }
        if (request.getResidentialAddress() != null) {
            user.setResidentialAddress(normalizeOptionalText(request.getResidentialAddress()));
        }
        if (request.isJoiningDateProvided()) {
            user.setJoiningDate(request.getJoiningDate());
        }
        if (request.getEmploymentStatus() != null) {
            user.setEmploymentStatus(normalizeOptionalText(request.getEmploymentStatus()));
        }
        if (request.getDesignation() != null) {
            user.setDesignation(normalizeOptionalText(request.getDesignation()));
        }
        if (request.getAdministrativeRole() != null) {
            user.setAdministrativeRole(normalizeOptionalText(request.getAdministrativeRole()));
        }
        if (request.getQualification() != null) {
            user.setQualification(normalizeOptionalText(request.getQualification()));
        }
        if (request.getSpecialization() != null) {
            user.setSpecialization(normalizeOptionalText(request.getSpecialization()));
        }
        if (request.isYearsOfExperienceProvided()) {
            user.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getEmploymentType() != null) {
            user.setEmploymentType(normalizeOptionalText(request.getEmploymentType()));
        }
        if (request.getDepartmentIds() != null) {
            Set<Department> selectedDepartments = new LinkedHashSet<>();
            request.getDepartmentIds().forEach(id -> selectedDepartments.add(departmentsById.get(id)));
            user.setDepartments(selectedDepartments);
            Long currentPrimaryId = user.getDepartment() == null ? null : user.getDepartment().getId();
            Long primaryId = request.getDepartmentIds().contains(currentPrimaryId)
                    ? currentPrimaryId
                    : request.getDepartmentIds().get(0);
            user.setDepartment(departmentsById.get(primaryId));
        }
    }

    private Map<Long, Department> validateFacultyProfileRequest(UpdateProfileRequest request) {
        requireText(request.getPhone(), "Phone is required.");
        if (!request.getPhone().trim().matches("^[6-9][0-9]{9}$")) {
            throw new RuntimeException("Phone must be a valid 10-digit Indian mobile number.");
        }
        requireText(request.getGender(), "Gender is required.");
        requireText(request.getEmploymentStatus(), "Employment status is required.");
        requireText(request.getDesignation(), "Academic designation is required.");
        requireText(request.getAdministrativeRole(), "Administrative role is required.");
        requireText(request.getQualification(), "Qualification is required.");
        requireText(request.getEmploymentType(), "Employment type is required.");
        if (request.getDepartmentIds() == null || request.getDepartmentIds().isEmpty()) {
            throw new RuntimeException("Select at least one department.");
        }
        if (request.getDepartmentIds().stream().anyMatch(Objects::isNull)) {
            throw new RuntimeException("Department IDs cannot be empty.");
        }
        if (new HashSet<>(request.getDepartmentIds()).size() != request.getDepartmentIds().size()) {
            throw new RuntimeException("Duplicate departments are not allowed.");
        }

        validateOption(FacultyProfileOption.Category.GENDER, request.getGender(), "Gender");
        validateOption(FacultyProfileOption.Category.ACADEMIC_DESIGNATION,
                request.getDesignation(), "Academic designation");
        validateOption(FacultyProfileOption.Category.ADMINISTRATIVE_ROLE,
                request.getAdministrativeRole(), "Administrative role");
        validateOption(FacultyProfileOption.Category.EMPLOYMENT_STATUS,
                request.getEmploymentStatus(), "Employment status");
        validateOption(FacultyProfileOption.Category.EMPLOYMENT_TYPE,
                request.getEmploymentType(), "Employment type");

        LocalDate today = LocalDate.now();
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(today)) {
            throw new RuntimeException("Date of birth cannot be in the future.");
        }
        if (!request.isJoiningDateProvided() || request.getJoiningDate() == null) {
            throw new RuntimeException("Joining date is required.");
        }
        if (request.getJoiningDate().isAfter(today)) {
            throw new RuntimeException("Joining date cannot be in the future.");
        }
        if (request.getDateOfBirth() != null
                && request.getJoiningDate().isBefore(request.getDateOfBirth())) {
            throw new RuntimeException("Joining date cannot be before date of birth.");
        }
        if (request.isYearsOfExperienceProvided()
                && request.getYearsOfExperience() != null
                && (!Double.isFinite(request.getYearsOfExperience())
                || request.getYearsOfExperience() < 0)) {
            throw new RuntimeException("Years of experience must be a non-negative number.");
        }
        if (request.getResidentialAddress() != null
                && request.getResidentialAddress().length() > 1000) {
            throw new RuntimeException("Residential address cannot exceed 1000 characters.");
        }
        List<Department> departments = departmentRepository.findAllById(request.getDepartmentIds());
        if (departments.size() != request.getDepartmentIds().size()) {
            throw new RuntimeException("One or more selected departments do not exist.");
        }
        return departments.stream().collect(Collectors.toMap(
                Department::getId,
                department -> department,
                (existing, duplicate) -> existing,
                LinkedHashMap::new));
    }

    private void validateOption(
            FacultyProfileOption.Category category,
            String value,
            String label) {
        if (!facultyProfileOptionRepository.existsByCategoryAndValue(
                category, value.trim())) {
            throw new RuntimeException("Select a valid " + label.toLowerCase() + ".");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
    }

    private String normalizeOptionalText(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, String> facultyProfileValues(User user) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", user.getName());
        values.put("email", user.getEmail());
        values.put("phone", user.getPhone());
        values.put("dateOfBirth", valueOf(user.getDateOfBirth()));
        values.put("gender", user.getGender());
        values.put("residentialAddress", user.getResidentialAddress());
        values.put("joiningDate", valueOf(user.getJoiningDate()));
        values.put("employmentStatus", user.getEmploymentStatus());
        values.put("academicDesignation", user.getDesignation());
        values.put("administrativeRole", user.getAdministrativeRole());
        values.put("qualification", user.getQualification());
        values.put("specialization", user.getSpecialization());
        values.put("yearsOfExperience", valueOf(user.getYearsOfExperience()));
        values.put("employmentType", user.getEmploymentType());
        Set<Long> departmentIds = user.getDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        if (departmentIds.isEmpty() && user.getDepartment() != null) {
            departmentIds.add(user.getDepartment().getId());
        }
        values.put("departments", departmentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        values.put("primaryDepartmentId", valueOf(
                user.getDepartment() == null ? null : user.getDepartment().getId()));
        return values;
    }

    private String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    private void recordFacultyProfileChanges(
            User user,
            String changedBy,
            Map<String, String> previousValues,
            Map<String, String> updatedValues) {
        LocalDateTime changedAt = LocalDateTime.now();
        List<FacultyProfileAudit> changes = previousValues.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getValue(), updatedValues.get(entry.getKey())))
                .map(entry -> new FacultyProfileAudit(
                        user.getId(),
                        changedBy,
                        entry.getKey(),
                        entry.getValue(),
                        updatedValues.get(entry.getKey()),
                        changedAt))
                .toList();
        if (!changes.isEmpty()) {
            facultyProfileAuditRepository.saveAll(changes);
        }
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
