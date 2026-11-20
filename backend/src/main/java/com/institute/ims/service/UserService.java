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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final long MAX_PROFILE_PICTURE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_PROFILE_PICTURE_PIXELS = 16_000_000L;
    private static final int MAX_PROFILE_PICTURE_DIMENSION = 8_192;

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

    @Transactional
    public User updateProfilePicture(String rollNumber, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a profile picture to upload.");
        }
        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile pictures must be 5 MB or smaller.");
        }

        byte[] image;
        try {
            image = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded profile picture.", exception);
        }

        byte[] normalizedImage = normalizeProfilePicture(image, file.getContentType());
        User user = getUserByRollNumber(rollNumber);
        long previousVersion = user.getProfilePictureVersion() == null
                ? 0
                : user.getProfilePictureVersion();
        user.setProfilePicture(normalizedImage);
        user.setProfilePictureContentType("image/jpeg");
        user.setProfilePictureVersion(Math.max(System.currentTimeMillis(), previousVersion + 1));
        return userRepository.save(user);
    }

    private byte[] normalizeProfilePicture(byte[] image, String contentType) {
        String normalizedContentType = contentType == null
                ? ""
                : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        String expectedFormat = switch (normalizedContentType) {
            case "image/jpeg" -> "jpeg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Profile pictures must be JPEG, PNG, or GIF images.");
        };

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            if (input == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!expectedFormat.equalsIgnoreCase(reader.getFormatName())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image content does not match its file type.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0
                        || height <= 0
                        || width > MAX_PROFILE_PICTURE_DIMENSION
                        || height > MAX_PROFILE_PICTURE_DIMENSION
                        || (long) width * height > MAX_PROFILE_PICTURE_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image dimensions must not exceed 16 megapixels.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
                }
                double scale = Math.min(1.0, 512.0 / Math.max(width, height));
                int normalizedWidth = Math.max(1, (int) Math.round(width * scale));
                int normalizedHeight = Math.max(1, (int) Math.round(height * scale));
                BufferedImage normalized = new BufferedImage(
                        normalizedWidth, normalizedHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = normalized.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, normalizedWidth, normalizedHeight);
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(decoded, 0, 0, normalizedWidth, normalizedHeight, null);
                graphics.dispose();

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(normalized, "jpeg", output)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not process the uploaded image.");
                }
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.", exception);
        }
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
        if (request.getAdministrativeRole() != null) {
            throw new AccessDeniedException("Administrative role can only be managed by an administrator.");
        }
        if (user.getRole() == Role.FACULTY && request.getDepartmentIds() != null) {
            throw new AccessDeniedException("Faculty departments are assigned through subject teaching approvals.");
        }
        boolean hasFacultyFields = request.getGender() != null
                || request.isJoiningDateProvided()
                || request.getEmploymentStatus() != null
                || request.getDesignation() != null
                || request.getQualification() != null
                || request.getSpecialization() != null
                || request.isYearsOfExperienceProvided()
                || request.getEmploymentType() != null
                || request.getDepartmentIds() != null;
        boolean hasStudentFields = request.getRegistrationNumber() != null
                || request.getSemesterSgpas() != null;
        boolean hasSharedPersonalFields = request.getPhone() != null
                || request.isDateOfBirthProvided()
                || request.getResidentialAddress() != null;
        if (user.getRole() != Role.FACULTY && hasFacultyFields) {
            throw new AccessDeniedException("Only faculty can update faculty profile details.");
        }
        if (user.getRole() != Role.STUDENT && hasStudentFields) {
            throw new AccessDeniedException("Only students can update student academic details.");
        }
        if (user.getRole() != Role.FACULTY && user.getRole() != Role.STUDENT && hasSharedPersonalFields) {
            throw new AccessDeniedException("Only faculty and students can update personal details.");
        }

        Map<String, String> previousFacultyProfile = null;
        if (user.getRole() == Role.FACULTY) {
            validateFacultyProfileRequest(request);
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
                || request.getOptionalSubjectCodes() != null
                || hasStudentFields;
        if (user.getRole() != Role.STUDENT) {
            if (hasAcademicFields) {
                throw new AccessDeniedException("Only students can update academic details.");
            }
            if (user.getRole() == Role.FACULTY) {
                updateFacultyProfileFields(user, request);
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

        updateStudentPersonalFields(user, request);
        if (request.getRegistrationNumber() != null) {
            String registrationNumber = normalizeOptionalText(request.getRegistrationNumber());
            if (registrationNumber != null
                    && userRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(
                            registrationNumber, user.getId())) {
                throw new RuntimeException("Registration number already exists.");
            }
            user.setRegistrationNumber(registrationNumber);
        }

        boolean academicSelectionProvided = request.getYear() != null || request.getSemester() != null;
        if (academicSelectionProvided && (request.getYear() == null || request.getSemester() == null)) {
            throw new RuntimeException("Academic year and semester must both be provided.");
        }
        if (academicSelectionProvided) {
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
        } else if (request.getOptionalSubjectCodes() != null
                && !request.getOptionalSubjectCodes().isEmpty()) {
            throw new RuntimeException("Select an academic year and semester before choosing subjects.");
        }

        Integer effectiveSemester = user.getSemester();
        if (request.getSemesterSgpas() != null) {
            if (effectiveSemester == null) {
                throw new RuntimeException("Select your current semester before entering previous semester SGPAs.");
            }
            user.setSemesterSgpas(validateSemesterSgpas(request.getSemesterSgpas(), effectiveSemester));
        } else {
            validateSemesterSgpas(user.getSemesterSgpas(), effectiveSemester);
        }
        return userRepository.save(user);
    }

    private void updateStudentPersonalFields(User user, UpdateProfileRequest request) {
        if (request.getPhone() != null) {
            String phone = normalizeOptionalText(request.getPhone());
            if (phone != null && !phone.matches("^[6-9][0-9]{9}$")) {
                throw new RuntimeException("Phone must be a valid 10-digit Indian mobile number.");
            }
            user.setPhone(phone);
        }
        if (request.isDateOfBirthProvided()) {
            LocalDate dateOfBirth = request.getDateOfBirth();
            if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
                throw new RuntimeException("Date of birth cannot be in the future.");
            }
            user.setDateOfBirth(dateOfBirth);
        }
        if (request.getResidentialAddress() != null) {
            user.setResidentialAddress(normalizeOptionalText(request.getResidentialAddress()));
        }
    }

    private Map<Integer, Double> validateSemesterSgpas(
            Map<Integer, Double> semesterSgpas,
            Integer currentSemester) {
        if (semesterSgpas.isEmpty()) {
            return new LinkedHashMap<>();
        }
        if (currentSemester == null) {
            throw new RuntimeException("Select your current semester before entering previous semester SGPAs.");
        }

        Map<Integer, Double> validatedSgpas = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : semesterSgpas.entrySet()) {
            Integer semesterNumber = entry.getKey();
            Double sgpa = entry.getValue();
            if (semesterNumber == null || semesterNumber < 1 || semesterNumber >= currentSemester
                    || semesterNumber > 8) {
                throw new RuntimeException("SGPA can only be entered for a previous semester.");
            }
            if (sgpa == null || !Double.isFinite(sgpa) || sgpa < 0 || sgpa > 10) {
                throw new RuntimeException("SGPA must be a number from 0 to 10.");
            }
            validatedSgpas.put(semesterNumber, sgpa);
        }
        return validatedSgpas;
    }

    private void updateFacultyProfileFields(
            User user,
            UpdateProfileRequest request) {
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
    }

    private void validateFacultyProfileRequest(UpdateProfileRequest request) {
        requireText(request.getPhone(), "Phone is required.");
        if (!request.getPhone().trim().matches("^[6-9][0-9]{9}$")) {
            throw new RuntimeException("Phone must be a valid 10-digit Indian mobile number.");
        }
        requireText(request.getGender(), "Gender is required.");
        requireText(request.getEmploymentStatus(), "Employment status is required.");
        requireText(request.getDesignation(), "Academic designation is required.");
        requireText(request.getQualification(), "Qualification is required.");
        requireText(request.getEmploymentType(), "Employment type is required.");
        validateOption(FacultyProfileOption.Category.GENDER, request.getGender(), "Gender");
        validateOption(FacultyProfileOption.Category.ACADEMIC_DESIGNATION,
                request.getDesignation(), "Academic designation");
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
