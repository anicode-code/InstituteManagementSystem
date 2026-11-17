package com.institute.ims.service;

import com.institute.ims.common.ResourceNotFoundException;
import com.institute.ims.dto.AdminFacultyProfile;
import com.institute.ims.dto.AdminFacultyRosterItem;
import com.institute.ims.dto.AdminFacultyRosterPage;
import com.institute.ims.model.*;
import com.institute.ims.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDepartmentFacultyService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    @Value("${app.students.attendance-threshold:75}")
    private double attendanceThreshold;

    @Value("${app.students.performance-threshold:5}")
    private double performanceThreshold;

    public AdminDepartmentFacultyService(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public AdminFacultyRosterPage getRoster(
            Long departmentId,
            Integer academicYear,
            Long subjectId,
            VerificationStatus status,
            String search,
            String sortBy,
            String direction,
            int page,
            int size) {
        return getRoster(
                departmentId, academicYear, subjectId, status, search, null, sortBy, direction, page, size);
    }

    @Transactional(readOnly = true)
    public AdminFacultyRosterPage getRoster(
            Long departmentId,
            Integer academicYear,
            Long subjectId,
            VerificationStatus status,
            String search,
            String sort,
            String sortBy,
            String direction,
            int page,
            int size) {
        validatePagination(page, size);
        validateAcademicYear(academicYear);
        List<SortCriterion> sortCriteria = parseSort(sort, sortBy, direction);
        DirectoryData directory = loadDirectory(departmentId, true);
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<FacultyRow> filteredFaculty = directory.facultyById().values().stream()
                .map(faculty -> {
                    List<Subject> assignedSubjects = directory.subjectsByFaculty()
                            .getOrDefault(faculty.getId(), List.of()).stream()
                            .filter(subject -> academicYear == null
                                    || Objects.equals(subject.getAcademicYear(), academicYear))
                            .filter(subject -> subjectId == null
                                    || Objects.equals(subject.getId(), subjectId))
                            .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                    return new FacultyRow(faculty, assignedSubjects);
                })
                .filter(row -> status == null || row.faculty().getStatus() == status)
                .filter(row -> subjectId == null || !row.subjects().isEmpty())
                .filter(row -> academicYear == null || !row.subjects().isEmpty())
                .filter(row -> query.isEmpty()
                        || contains(row.faculty().getName(), query)
                        || contains(row.faculty().getRollNumber(), query)
                        || contains(row.faculty().getEmail(), query))
                .toList();

        Comparator<FacultyRow> comparator = rosterComparator(sortCriteria, directory);
        List<FacultyRow> sortedFaculty = filteredFaculty.stream()
                .sorted(comparator.thenComparing(row -> row.faculty().getRollNumber(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        Set<Long> visibleSubjectIds = sortedFaculty.stream()
                .flatMap(row -> row.subjects().stream())
                .map(Subject::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        AdminFacultyRosterPage.Summary summary = summarize(sortedFaculty, visibleSubjectIds, directory);

        int from = (int) Math.min((long) page * size, sortedFaculty.size());
        int to = Math.min(from + size, sortedFaculty.size());
        int totalPages = (int) Math.ceil(sortedFaculty.size() / (double) size);
        List<AdminFacultyRosterItem> items = sortedFaculty.subList(from, to).stream()
                .map(row -> toRosterItem(row, directory))
                .toList();

        return new AdminFacultyRosterPage(
                items,
                summary,
                filterOptions(directory),
                page,
                size,
                sortedFaculty.size(),
                totalPages);
    }

    @Transactional(readOnly = true)
    public AdminFacultyProfile getProfile(Long departmentId, Long facultyId) {
        DirectoryData directory = loadDirectory(departmentId, false);
        User faculty = directory.facultyById().get(facultyId);
        if (faculty == null) {
            if (userRepository.existsById(facultyId)) {
                throw new AccessDeniedException("You may only view faculty associated with this department.");
            }
            throw new ResourceNotFoundException("Faculty member not found.");
        }

        List<Subject> subjects = directory.subjectsByFaculty()
                .getOrDefault(facultyId, List.of()).stream()
                .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Set<Long> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toSet());
        DirectoryData profileData = loadAttendance(directory, subjectIds);
        List<Attendance> attendance = attendanceForSubjects(subjectIds, profileData);
        List<AdminFacultyProfile.AssignedSubject> assignedSubjects = subjects.stream()
                .map(subject -> toAssignedSubject(subject, profileData))
                .toList();

        List<Assignment> assignments = subjectIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByFacultyIdAndSubjectIdIn(facultyId, new ArrayList<>(subjectIds));
        Set<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toSet());
        List<AssignmentSubmission> submissions = assignmentIds.isEmpty()
                ? List.of()
                : submissionRepository.findByAssignmentIdIn(new ArrayList<>(assignmentIds));
        Map<Long, List<AssignmentSubmission>> submissionsByAssignment = submissions.stream()
                .collect(Collectors.groupingBy(submission -> submission.getAssignment().getId()));
        List<AdminFacultyProfile.AssignmentSummary> assignmentSummaries = assignments.stream()
                .sorted(Comparator.comparing(Assignment::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(assignment -> toAssignmentSummary(
                        assignment,
                        submissionsByAssignment.getOrDefault(assignment.getId(), List.of()),
                        profileData))
                .toList();

        List<Integer> academicYears = subjects.stream()
                .map(Subject::getAcademicYear)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<Integer> semesters = subjects.stream()
                .map(Subject::getSemester)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        Long classesConducted = totalClasses(subjects, profileData);
        int studentsTeaching = enrolledStudentIds(subjects, profileData).size();
        Double overallAttendance = attendancePercentage(attendance);

        Department department = profileData.department();
        AdminFacultyProfile.PersonalInformation personalInformation =
                new AdminFacultyProfile.PersonalInformation(
                        faculty.getName(),
                        null,
                        faculty.getRollNumber(),
                        faculty.getEmail(),
                        faculty.getPhone(),
                        faculty.getDateOfBirth(),
                        faculty.getGender(),
                        faculty.getResidentialAddress(),
                        faculty.getJoiningDate(),
                        faculty.getCreatedAt(),
                        faculty.getStatus(),
                        faculty.getEmploymentStatus());
        AdminFacultyProfile.ProfessionalInformation professionalInformation =
                new AdminFacultyProfile.ProfessionalInformation(
                        department.getName(),
                        department.getCode(),
                        faculty.getDesignation(),
                        faculty.getAdministrativeRole(),
                        faculty.getQualification(),
                        faculty.getSpecialization(),
                        faculty.getYearsOfExperience(),
                        faculty.getEmploymentType(),
                        academicYears,
                        semesters);
        AdminFacultyProfile.TeachingOverview teachingOverview =
                new AdminFacultyProfile.TeachingOverview(
                        subjects.size(),
                        classesConducted,
                        studentsTeaching,
                        academicYears,
                        semesters);

        List<AdminFacultyProfile.SubjectAttendance> subjectAttendance = subjects.stream()
                .map(subject -> toSubjectAttendance(subject, profileData))
                .toList();
        List<AdminFacultyProfile.AttendanceRecord> attendanceRecords = attendance.stream()
                .sorted(Comparator.comparing(
                        Attendance::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toAttendanceRecord)
                .toList();
        AdminFacultyProfile.AttendanceOverview attendanceOverview =
                new AdminFacultyProfile.AttendanceOverview(
                        classesConducted,
                        attendance.size(),
                        overallAttendance,
                        subjectAttendance,
                        attendanceRecords);
        AdminFacultyProfile.AcademicPerformanceOverview performanceOverview =
                performanceOverview(subjects, attendance, profileData);

        return new AdminFacultyProfile(
                personalInformation,
                professionalInformation,
                teachingOverview,
                assignedSubjects,
                attendanceOverview,
                assignmentSummaries,
                performanceOverview,
                new AdminFacultyProfile.AdditionalInformation(List.of()));
    }

    private DirectoryData loadDirectory(Long departmentId, boolean includeAttendance) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
        List<Subject> departmentSubjects = subjectRepository.findByDepartmentIdWithFacultyMembers(departmentId);
        Map<Long, User> facultyById = new LinkedHashMap<>();
        userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, departmentId)
                .forEach(faculty -> facultyById.put(faculty.getId(), faculty));

        Map<Long, List<Subject>> subjectsByFaculty = new HashMap<>();
        for (Subject subject : departmentSubjects) {
            Set<User> assignedFaculty = subject.getFacultyMembers();
            if (assignedFaculty == null || assignedFaculty.isEmpty()) {
                User legacyFaculty = subject.getFaculty();
                if (legacyFaculty == null) {
                    continue;
                }
                assignedFaculty = Set.of(legacyFaculty);
            }
            for (User faculty : assignedFaculty) {
                if (faculty.getRole() != Role.FACULTY && faculty.getRole() != Role.HOD) {
                    continue;
                }
                facultyById.putIfAbsent(faculty.getId(), faculty);
                subjectsByFaculty.computeIfAbsent(faculty.getId(), ignored -> new ArrayList<>()).add(subject);
            }
        }

        List<User> departmentStudents = userRepository.findByRoleAndDepartmentId(Role.STUDENT, departmentId);
        List<Long> studentIds = departmentStudents.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();
        List<User> studentsWithSubjects = studentIds.isEmpty()
                ? List.of()
                : userRepository.findWithSelectedSubjectsByIdIn(studentIds);
        Map<Long, User> activeStudents = studentsWithSubjects.stream()
                .filter(student -> student.getStatus() == VerificationStatus.VERIFIED)
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, ignored) -> first));

        Set<Long> assignedSubjectIds = subjectsByFaculty.values().stream()
                .flatMap(Collection::stream)
                .map(Subject::getId)
                .collect(Collectors.toSet());
        Set<Long> departmentStudentIds = new HashSet<>(studentIds);
        Map<Long, Set<Long>> enrolledStudentsBySubject = new HashMap<>();
        for (List<Subject> assignedSubjects : subjectsByFaculty.values()) {
            for (Subject subject : assignedSubjects) {
                Set<Long> enrolled = activeStudents.values().stream()
                        .filter(student -> isEnrolledIn(student, subject, departmentId))
                        .map(User::getId)
                        .collect(Collectors.toSet());
                enrolledStudentsBySubject.put(subject.getId(), enrolled);
            }
        }

        DirectoryData directory = new DirectoryData(
                department,
                facultyById,
                subjectsByFaculty,
                departmentStudentIds,
                enrolledStudentsBySubject,
                Map.of());
        return includeAttendance
                ? loadAttendance(directory, assignedSubjectIds)
                : directory;
    }

    private DirectoryData loadAttendance(DirectoryData directory, Set<Long> subjectIds) {
        Map<Long, List<Attendance>> attendanceBySubject = new HashMap<>();
        subjectIds.forEach(subjectId -> attendanceBySubject.put(subjectId, new ArrayList<>()));
        if (!subjectIds.isEmpty()) {
            attendanceRepository.findBySubjectIdIn(new ArrayList<>(subjectIds)).stream()
                    .filter(record -> record.getStudent() != null
                            && record.getSubject() != null
                            && directory.departmentStudentIds().contains(record.getStudent().getId()))
                    .forEach(record -> attendanceBySubject
                            .computeIfAbsent(record.getSubject().getId(), ignored -> new ArrayList<>())
                            .add(record));
        }
        return new DirectoryData(
                directory.department(),
                directory.facultyById(),
                directory.subjectsByFaculty(),
                directory.departmentStudentIds(),
                directory.enrolledStudentsBySubject(),
                attendanceBySubject);
    }

    private boolean isEnrolledIn(User student, Subject subject, Long departmentId) {
        boolean explicitlySelected = student.getSelectedSubjects() != null
                && student.getSelectedSubjects().stream()
                        .anyMatch(selected -> Objects.equals(selected.getId(), subject.getId()));
        boolean compulsoryEnrollment = subject.getType() == SubjectType.COMPULSORY
                && student.getDepartment() != null
                && Objects.equals(student.getDepartment().getId(), departmentId)
                && Objects.equals(student.getSemester(), subject.getSemester());
        return explicitlySelected || compulsoryEnrollment;
    }

    private AdminFacultyRosterPage.Summary summarize(
            List<FacultyRow> facultyRows,
            Set<Long> visibleSubjectIds,
            DirectoryData directory) {
        Set<Long> subjectIds = visibleSubjectIds;
        int activeFaculty = (int) facultyRows.stream()
                .filter(row -> row.faculty().getStatus() == VerificationStatus.VERIFIED)
                .count();
        return new AdminFacultyRosterPage.Summary(
                facultyRows.size(),
                activeFaculty,
                subjectIds.size(),
                attendancePercentage(attendanceForSubjects(subjectIds, directory)));
    }

    private AdminFacultyRosterPage.FilterOptions filterOptions(DirectoryData directory) {
        List<Subject> subjects = directory.subjectsByFaculty().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (first, ignored) -> first))
                .values().stream()
                .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<AdminFacultyRosterPage.SubjectOption> subjectOptions = subjects.stream()
                .map(subject -> new AdminFacultyRosterPage.SubjectOption(
                        subject.getId(),
                        subject.getName(),
                        subject.getCode(),
                        subject.getAcademicYear(),
                        subject.getSemester()))
                .toList();
        List<Integer> years = subjects.stream()
                .map(Subject::getAcademicYear)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return new AdminFacultyRosterPage.FilterOptions(
                subjectOptions,
                years,
                Arrays.asList(VerificationStatus.values()));
    }

    private AdminFacultyRosterItem toRosterItem(FacultyRow row, DirectoryData directory) {
        return new AdminFacultyRosterItem(
                row.faculty().getId(),
                row.faculty().getRollNumber(),
                row.faculty().getName(),
                null,
                row.faculty().getDesignation(),
                row.faculty().getEmail(),
                row.faculty().getPhone(),
                row.subjects().size(),
                enrolledStudentIds(row.subjects(), directory).size(),
                row.faculty().getStatus(),
                attendancePercentage(attendanceForSubjects(subjectIds(row.subjects()), directory)));
    }

    private AdminFacultyProfile.AssignedSubject toAssignedSubject(
            Subject subject,
            DirectoryData directory) {
        List<Attendance> attendance = directory.attendanceBySubject()
                .getOrDefault(subject.getId(), List.of());
        return new AdminFacultyProfile.AssignedSubject(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                subject.getAcademicYear(),
                subject.getSemester(),
                directory.enrolledStudentsBySubject().getOrDefault(subject.getId(), Set.of()).size(),
                classesConducted(subject, directory),
                attendancePercentage(attendance),
                averagePerformance(attendance));
    }

    private AdminFacultyProfile.SubjectAttendance toSubjectAttendance(
            Subject subject,
            DirectoryData directory) {
        List<Attendance> records = directory.attendanceBySubject()
                .getOrDefault(subject.getId(), List.of());
        return new AdminFacultyProfile.SubjectAttendance(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                classesConducted(subject, directory),
                directory.enrolledStudentsBySubject().getOrDefault(subject.getId(), Set.of()).size(),
                records.size(),
                attendancePercentage(records));
    }

    private AdminFacultyProfile.AttendanceRecord toAttendanceRecord(Attendance record) {
        User student = record.getStudent();
        Subject subject = record.getSubject();
        return new AdminFacultyProfile.AttendanceRecord(
                record.getId(),
                record.getDate(),
                student.getId(),
                student.getName(),
                student.getRollNumber(),
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                record.getStatus(),
                record.getPerformanceMarks());
    }

    private AdminFacultyProfile.AssignmentSummary toAssignmentSummary(
            Assignment assignment,
            List<AssignmentSubmission> submissions,
            DirectoryData directory) {
        Set<Long> enrolledStudentIds = directory.enrolledStudentsBySubject()
                .getOrDefault(assignment.getSubject().getId(), Set.of());
        long relevantSubmissions = submissions.stream()
                .filter(submission -> submission.getStudent() != null)
                .map(submission -> submission.getStudent().getId())
                .distinct()
                .count();
        long evaluated = submissions.stream()
                .filter(submission -> submission.getStudent() != null)
                .filter(submission -> submission.getStatus() == SubmissionStatus.EVALUATED)
                .map(submission -> submission.getStudent().getId())
                .distinct()
                .count();
        String gradingStatus = relevantSubmissions == 0
                ? "NO SUBMISSIONS"
                : evaluated == relevantSubmissions
                        ? "GRADED"
                        : evaluated == 0 ? "PENDING GRADING" : "PARTIALLY GRADED";
        long pending = Math.max(0,
                enrolledStudentIds.size() - relevantSubmissions);
        return new AdminFacultyProfile.AssignmentSummary(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getSubject().getName(),
                assignment.getSubject().getCode(),
                null,
                assignment.getDueDate(),
                Math.toIntExact(relevantSubmissions),
                Math.toIntExact(pending),
                gradingStatus,
                assignment.getMaxMarks());
    }

    private AdminFacultyProfile.AcademicPerformanceOverview performanceOverview(
            List<Subject> subjects,
            List<Attendance> attendance,
            DirectoryData directory) {
        List<AdminFacultyProfile.SubjectPerformance> subjectPerformance = subjects.stream()
                .map(subject -> {
                    List<Attendance> records = directory.attendanceBySubject()
                            .getOrDefault(subject.getId(), List.of());
                    long assessedRecords = records.stream()
                            .filter(record -> record.getPerformanceMarks() != null)
                            .count();
                    return new AdminFacultyProfile.SubjectPerformance(
                            subject.getId(),
                            subject.getName(),
                            subject.getCode(),
                            averagePerformance(records),
                            Math.toIntExact(assessedRecords));
                })
                .toList();

        Map<Long, List<Attendance>> byStudent = attendance.stream()
                .filter(record -> record.getStudent() != null)
                .collect(Collectors.groupingBy(record -> record.getStudent().getId()));
        List<AdminFacultyProfile.StudentRequiringAttention> studentsRequiringAttention = byStudent.entrySet()
                .stream()
                .map(entry -> toStudentAttention(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        AdminFacultyProfile.StudentRequiringAttention::studentName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new AdminFacultyProfile.AcademicPerformanceOverview(
                averagePerformance(attendance),
                subjectPerformance,
                studentsRequiringAttention,
                null);
    }

    private AdminFacultyProfile.StudentRequiringAttention toStudentAttention(
            Long studentId,
            List<Attendance> attendance) {
        if (attendance.isEmpty()) {
            return null;
        }
        Double attendancePercentage = attendancePercentage(attendance);
        Double averagePerformance = averagePerformance(attendance);
        List<String> reasons = new ArrayList<>();
        if (attendancePercentage != null && attendancePercentage < attendanceThreshold) {
            reasons.add("LOW ATTENDANCE");
        }
        if (averagePerformance != null && averagePerformance < performanceThreshold) {
            reasons.add("POOR PERFORMANCE");
        }
        if (reasons.isEmpty()) {
            return null;
        }
        User student = attendance.get(0).getStudent();
        List<String> subjects = attendance.stream()
                .map(Attendance::getSubject)
                .filter(Objects::nonNull)
                .map(Subject::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return new AdminFacultyProfile.StudentRequiringAttention(
                studentId,
                student.getName(),
                student.getRollNumber(),
                attendancePercentage,
                averagePerformance,
                reasons,
                subjects);
    }

    private Comparator<FacultyRow> rosterComparator(
            List<SortCriterion> sortCriteria,
            DirectoryData directory) {
        Comparator<FacultyRow> comparator = null;
        for (SortCriterion criterion : sortCriteria) {
            boolean descending = "desc".equals(criterion.direction());
            Comparator<FacultyRow> next = switch (criterion.field()) {
                case "facultyId" -> nullableFacultyComparator(row -> row.faculty().getRollNumber(), descending, String.CASE_INSENSITIVE_ORDER);
                case "name" -> nullableFacultyComparator(row -> row.faculty().getName(), descending, String.CASE_INSENSITIVE_ORDER);
                case "designation" -> nullableFacultyComparator(row -> null, descending, String.CASE_INSENSITIVE_ORDER);
                case "email" -> nullableFacultyComparator(row -> row.faculty().getEmail(), descending, String.CASE_INSENSITIVE_ORDER);
                case "phone" -> nullableFacultyComparator(row -> null, descending, String.CASE_INSENSITIVE_ORDER);
                case "subjects" -> Comparator.comparingInt(row -> row.subjects().size());
                case "students" -> Comparator.comparingInt(row -> enrolledStudentIds(row.subjects(), directory).size());
                case "attendance" -> nullableFacultyComparator(
                        row -> attendancePercentage(attendanceForSubjects(subjectIds(row.subjects()), directory)),
                        descending,
                        Comparator.<Double>naturalOrder());
                case "status" -> nullableFacultyComparator(
                        row -> row.faculty().getStatus() == null ? null : row.faculty().getStatus().name(),
                        descending,
                        String.CASE_INSENSITIVE_ORDER);
                default -> throw new IllegalArgumentException("Unsupported faculty sort field.");
            };
            if (descending && Set.of("subjects", "students").contains(criterion.field())) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator;
    }

    private List<SortCriterion> parseSort(String sort, String defaultSortBy, String defaultDirection) {
        if (sort == null || sort.isBlank()) {
            validateSortOptions(defaultSortBy, defaultDirection);
            return List.of(new SortCriterion(defaultSortBy, defaultDirection.toLowerCase(Locale.ROOT)));
        }
        return Arrays.stream(sort.split(","))
                .map(value -> value.split(":", -1))
                .map(parts -> {
                    if (parts.length != 2 || !Set.of(
                            "facultyId", "name", "designation", "email", "phone",
                            "subjects", "students", "attendance", "status").contains(parts[0])) {
                        throw new IllegalArgumentException("Unsupported faculty sort field.");
                    }
                    String direction = parts[1].toLowerCase(Locale.ROOT);
                    if (!Set.of("asc", "desc").contains(direction)) {
                        throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
                    }
                    return new SortCriterion(parts[0], direction);
                })
                .toList();
    }

    private <T> Comparator<FacultyRow> nullableFacultyComparator(
            Function<FacultyRow, T> extractor,
            boolean descending,
            Comparator<T> order) {
        Comparator<T> selectedOrder = descending ? order.reversed() : order;
        return Comparator.comparing(extractor, Comparator.nullsLast(selectedOrder));
    }

    private record SortCriterion(String field, String direction) {}

    private List<Attendance> attendanceForSubjects(Set<Long> subjectIds, DirectoryData directory) {
        return subjectIds.stream()
                .flatMap(subjectId -> directory.attendanceBySubject()
                        .getOrDefault(subjectId, List.of()).stream())
                .toList();
    }

    private Set<Long> subjectIds(List<Subject> subjects) {
        return subjects.stream().map(Subject::getId).collect(Collectors.toSet());
    }

    private Set<Long> enrolledStudentIds(List<Subject> subjects, DirectoryData directory) {
        return subjects.stream()
                .flatMap(subject -> directory.enrolledStudentsBySubject()
                        .getOrDefault(subject.getId(), Set.of()).stream())
                .collect(Collectors.toSet());
    }

    private Long classesConducted(Subject subject, DirectoryData directory) {
        Set<LocalDate> dates = directory.attendanceBySubject()
                .getOrDefault(subject.getId(), List.of()).stream()
                .map(Attendance::getDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return dates.isEmpty() ? null : (long) dates.size();
    }

    private Long totalClasses(List<Subject> subjects, DirectoryData directory) {
        long total = 0;
        boolean hasAttendance = false;
        for (Subject subject : subjects) {
            Long subjectClasses = classesConducted(subject, directory);
            if (subjectClasses != null) {
                total += subjectClasses;
                hasAttendance = true;
            }
        }
        return hasAttendance ? total : null;
    }

    private Double attendancePercentage(List<Attendance> records) {
        if (records.isEmpty()) {
            return null;
        }
        long present = records.stream()
                .filter(record -> record.getStatus() == AttendanceStatus.PRESENT)
                .count();
        return round(present * 100.0 / records.size());
    }

    private Double averagePerformance(List<Attendance> records) {
        List<Integer> marks = records.stream()
                .map(Attendance::getPerformanceMarks)
                .filter(Objects::nonNull)
                .toList();
        return marks.isEmpty()
                ? null
                : round(marks.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    private Double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private void validateAcademicYear(Integer academicYear) {
        if (academicYear != null && (academicYear < 1 || academicYear > 4)) {
            throw new IllegalArgumentException("Academic year must be between 1 and 4.");
        }
    }

    private void validateSortOptions(String sortBy, String direction) {
        if (sortBy == null || !Set.of("facultyId", "name", "email", "subjects", "students", "attendance", "status")
                .contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported faculty sort field.");
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }
    }

    private record FacultyRow(User faculty, List<Subject> subjects) {
    }

    private record DirectoryData(
            Department department,
            Map<Long, User> facultyById,
            Map<Long, List<Subject>> subjectsByFaculty,
            Set<Long> departmentStudentIds,
            Map<Long, Set<Long>> enrolledStudentsBySubject,
            Map<Long, List<Attendance>> attendanceBySubject) {
    }
}
