package com.institute.ims.service;

import com.institute.ims.common.ResourceNotFoundException;
import com.institute.ims.dto.AdminStudentProfile;
import com.institute.ims.dto.AdminStudentRosterItem;
import com.institute.ims.dto.AdminStudentRosterPage;
import com.institute.ims.model.*;
import com.institute.ims.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDepartmentStudentsService {

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

    public AdminDepartmentStudentsService(
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
    public AdminStudentRosterPage getRoster(
            Long departmentId,
            Integer year,
            Integer semester,
            VerificationStatus status,
            String search,
            String attention,
            String sortBy,
            String direction,
            int page,
            int size) {
        return getRoster(
                departmentId, year, semester, status, search, attention, null, sortBy, direction, page, size);
    }

    @Transactional(readOnly = true)
    public AdminStudentRosterPage getRoster(
            Long departmentId,
            Integer year,
            Integer semester,
            VerificationStatus status,
            String search,
            String attention,
            String sort,
            String sortBy,
            String direction,
            int page,
            int size) {
        requireDepartment(departmentId);
        validatePagination(page, size);
            validateAcademicFilters(year, semester);
            validateAttentionFilter(attention);
        List<User> departmentStudents = userRepository.findByRoleAndDepartmentId(Role.STUDENT, departmentId);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<User> filteredStudents = departmentStudents.stream()
                .filter(student -> year == null || Objects.equals(student.getYear(), year))
                .filter(student -> semester == null || Objects.equals(student.getSemester(), semester))
                .filter(student -> status == null || student.getStatus() == status)
                .filter(student -> normalizedSearch.isEmpty()
                        || contains(student.getName(), normalizedSearch)
                        || contains(student.getRollNumber(), normalizedSearch)
                        || contains(student.getEmail(), normalizedSearch))
                .toList();

        Map<Long, StudentData> studentData = loadStudentData(filteredStudents, departmentId);
        List<AdminStudentRosterItem> roster = filteredStudents.stream()
                .map(student -> toRosterItem(studentData.get(student.getId())))
                .toList();
        AdminStudentRosterPage.Summary summary = summarize(roster, studentData);

        List<AdminStudentRosterItem> visibleStudents = roster.stream()
                .filter(student -> matchesAttention(student, attention))
                .sorted(rosterComparator(sort, sortBy, direction))
                .toList();
        long fromIndex = (long) page * size;
        int from = (int) Math.min(fromIndex, visibleStudents.size());
        int to = Math.min(from + size, visibleStudents.size());
        int totalPages = (int) Math.ceil(visibleStudents.size() / (double) size);

        return new AdminStudentRosterPage(
                visibleStudents.subList(from, to),
                summary,
                page,
                size,
                visibleStudents.size(),
                totalPages,
                attendanceThreshold,
                performanceThreshold);
    }

    @Transactional(readOnly = true)
    public AdminStudentProfile getStudentProfile(Long departmentId, Long studentId) {
        requireDepartment(departmentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        if (student.getRole() != Role.STUDENT
                || student.getDepartment() == null
                || !Objects.equals(student.getDepartment().getId(), departmentId)) {
            throw new AccessDeniedException("You may only view students in this department.");
        }

        StudentData data = loadStudentData(List.of(student), departmentId).get(studentId);
        List<Attendance> attendance = data.attendance();
        List<Integer> performanceMarks = performanceMarks(attendance);
        long attended = countByStatus(attendance, AttendanceStatus.PRESENT);
        long missed = attendance.size() - attended;
        Double attendancePercentage = percentage(attended, attendance.size());
        Double averagePerformance = average(performanceMarks);
        long submittedAssignments = data.submissionsByAssignmentId().size();
        long pendingAssignments = data.assignments().size() - submittedAssignments;

        Department department = student.getDepartment();
        List<AdminStudentProfile.SubjectPerformance> subjectPerformance = data.subjects().stream()
                .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .map(subject -> toSubjectPerformance(subject, attendance))
                .toList();
        List<AdminStudentProfile.AttendanceEntry> attendanceDetails = attendance.stream()
                .sorted(Comparator.comparing(Attendance::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(record -> new AdminStudentProfile.AttendanceEntry(
                        record.getId(),
                        record.getSubject().getName(),
                        record.getSubject().getCode(),
                        record.getDate(),
                        record.getStatus(),
                        record.getPerformanceMarks()))
                .toList();
        List<AdminStudentProfile.AssignmentEntry> assignmentHistory = data.assignments().stream()
                .sorted(Comparator.comparing(Assignment::getDueDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Assignment::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(assignment -> toAssignmentEntry(
                        assignment,
                        data.submissionsByAssignmentId().get(assignment.getId())))
                .toList();

        return new AdminStudentProfile(
                new AdminStudentProfile.PersonalInformation(
                        student.getName(), null, student.getEmail(), null, null, null),
                new AdminStudentProfile.AcademicInformation(
                        student.getRollNumber(),
                        null,
                        department.getName(),
                        department.getCode(),
                        null,
                        student.getYear(),
                        student.getSemester(),
                        null,
                        student.getStatus(),
                        student.getCreatedAt()),
                new AdminStudentProfile.AcademicSummary(
                        attendancePercentage,
                        attended,
                        missed,
                        attendance.size(),
                        averagePerformance,
                        submittedAssignments,
                        data.assignments().size(),
                        pendingAssignments,
                        null,
                        null),
                subjectPerformance,
                attendanceDetails,
                assignmentHistory,
                List.of());
    }

    private Map<Long, StudentData> loadStudentData(List<User> students, Long departmentId) {
        if (students.isEmpty()) {
            return Map.of();
        }

        List<Long> studentIds = students.stream().map(User::getId).toList();
        Map<Long, User> usersWithSubjects = userRepository.findWithSelectedSubjectsByIdIn(studentIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, ignored) -> first));

        Map<Long, Set<Subject>> subjectsByStudent = new HashMap<>();
        for (User student : students) {
            User loadedStudent = usersWithSubjects.getOrDefault(student.getId(), student);
            Set<Subject> subjects = loadedStudent.getSelectedSubjects().stream()
                    .filter(subject -> subject.getDepartment() != null
                            && Objects.equals(subject.getDepartment().getId(), departmentId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            subjectsByStudent.put(student.getId(), subjects);
        }

        List<Integer> semesters = students.stream()
                .map(User::getSemester)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!semesters.isEmpty()) {
            Map<Integer, List<Subject>> compulsoryBySemester = subjectRepository
                    .findByDepartmentIdAndSemesterIn(departmentId, semesters)
                    .stream()
                    .filter(subject -> subject.getType() == SubjectType.COMPULSORY)
                    .collect(Collectors.groupingBy(Subject::getSemester));
            for (User student : students) {
                if (student.getSemester() != null) {
                    subjectsByStudent.get(student.getId()).addAll(
                            compulsoryBySemester.getOrDefault(student.getSemester(), List.of()));
                }
            }
        }

        Map<Long, Set<Long>> subjectIdsByStudent = new HashMap<>();
        Set<Long> allSubjectIds = new HashSet<>();
        for (Map.Entry<Long, Set<Subject>> entry : subjectsByStudent.entrySet()) {
            Set<Long> subjectIds = entry.getValue().stream()
                    .map(Subject::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            subjectIdsByStudent.put(entry.getKey(), subjectIds);
            allSubjectIds.addAll(subjectIds);
        }

        Map<Long, List<Attendance>> attendanceByStudent = new HashMap<>();
        List<Attendance> allAttendance = attendanceRepository.findByStudentIdIn(studentIds);
        Map<Long, List<Attendance>> attendanceByStudentAndSubject = allAttendance.stream()
                .filter(record -> record.getSubject() != null && record.getSubject().getId() != null)
                .filter(record -> subjectIdsByStudent.getOrDefault(
                        record.getStudent().getId(), Set.of()).contains(record.getSubject().getId()))
                .collect(Collectors.groupingBy(record -> record.getStudent().getId()));
        for (Long studentId : studentIds) {
            attendanceByStudent.put(studentId, attendanceByStudentAndSubject
                    .getOrDefault(studentId, List.of()));
        }

        List<Assignment> allAssignments = allSubjectIds.isEmpty()
                ? List.of()
                : assignmentRepository.findBySubjectIdIn(new ArrayList<>(allSubjectIds));
        Map<Long, List<Assignment>> assignmentsBySubject = allAssignments.stream()
                .filter(assignment -> assignment.getSubject() != null)
                .collect(Collectors.groupingBy(assignment -> assignment.getSubject().getId()));
        Map<Long, List<Assignment>> assignmentsByStudent = new HashMap<>();
        Map<Long, Set<Long>> assignmentIdsByStudent = new HashMap<>();
        for (Map.Entry<Long, Set<Subject>> entry : subjectsByStudent.entrySet()) {
            Map<Long, Assignment> uniqueAssignments = new LinkedHashMap<>();
            for (Subject subject : entry.getValue()) {
                for (Assignment assignment : assignmentsBySubject.getOrDefault(subject.getId(), List.of())) {
                    uniqueAssignments.put(assignment.getId(), assignment);
                }
            }
            List<Assignment> assignments = new ArrayList<>(uniqueAssignments.values());
            assignmentsByStudent.put(entry.getKey(), assignments);
            assignmentIdsByStudent.put(entry.getKey(), new HashSet<>(uniqueAssignments.keySet()));
        }

        Map<Long, Map<Long, AssignmentSubmission>> submissionsByStudent = new HashMap<>();
        if (assignmentIdsByStudent.values().stream().anyMatch(ids -> !ids.isEmpty())) {
            for (AssignmentSubmission submission : submissionRepository.findByStudentIdIn(studentIds)) {
                Long studentId = submission.getStudent().getId();
                Long assignmentId = submission.getAssignment().getId();
                if (assignmentIdsByStudent.getOrDefault(studentId, Set.of()).contains(assignmentId)) {
                    submissionsByStudent.computeIfAbsent(studentId, ignored -> new HashMap<>())
                            .putIfAbsent(assignmentId, submission);
                }
            }
        }

        Map<Long, StudentData> result = new HashMap<>();
        for (User student : students) {
            List<Subject> subjects = new ArrayList<>(subjectsByStudent.get(student.getId()));
            Map<Long, AssignmentSubmission> submissions = submissionsByStudent
                    .getOrDefault(student.getId(), Map.of());
            result.put(student.getId(), new StudentData(
                    student,
                    subjects,
                    attendanceByStudent.get(student.getId()),
                    assignmentsByStudent.getOrDefault(student.getId(), List.of()),
                    submissions));
        }
        return result;
    }

    private AdminStudentRosterItem toRosterItem(StudentData data) {
        List<Attendance> attendance = data.attendance();
        Double attendancePercentage = percentage(
                countByStatus(attendance, AttendanceStatus.PRESENT), attendance.size());
        Double academicPerformance = average(performanceMarks(attendance));
        long submittedAssignments = data.submissionsByAssignmentId().size();
        long pendingAssignments = data.assignments().size() - submittedAssignments;
        boolean lowAttendance = attendancePercentage != null
                && attendancePercentage < attendanceThreshold;
        boolean poorPerformance = academicPerformance != null
                && academicPerformance < performanceThreshold;
        boolean decliningPerformance = isPerformanceDeclining(attendance);
        boolean requiresAttention = lowAttendance || poorPerformance
                || pendingAssignments >= 2 || decliningPerformance;

        User student = data.student();
        return new AdminStudentRosterItem(
                student.getId(),
                student.getName(),
                student.getRollNumber(),
                student.getEmail(),
                student.getYear(),
                student.getSemester(),
                student.getStatus(),
                student.getCreatedAt(),
                attendancePercentage,
                academicPerformance,
                submittedAssignments,
                data.assignments().size(),
                pendingAssignments,
                lowAttendance,
                poorPerformance,
                decliningPerformance,
                requiresAttention);
    }

    private AdminStudentRosterPage.Summary summarize(
            List<AdminStudentRosterItem> roster,
            Map<Long, StudentData> studentData) {
        long totalClasses = studentData.values().stream()
                .mapToLong(data -> data.attendance().size())
                .sum();
        long attendedClasses = studentData.values().stream()
                .mapToLong(data -> countByStatus(data.attendance(), AttendanceStatus.PRESENT))
                .sum();
        List<Integer> marks = studentData.values().stream()
                .flatMap(data -> performanceMarks(data.attendance()).stream())
                .toList();
        Double averageAttendance = percentage(attendedClasses, totalClasses);
        Double averageAcademicPerformance = average(marks);
        long requiringAttention = roster.stream()
                .filter(AdminStudentRosterItem::requiresAttention)
                .count();
        return new AdminStudentRosterPage.Summary(
                roster.size(),
                averageAttendance,
                averageAcademicPerformance,
                requiringAttention);
    }

    private AdminStudentProfile.SubjectPerformance toSubjectPerformance(
            Subject subject,
            List<Attendance> attendance) {
        List<Attendance> subjectAttendance = attendance.stream()
                .filter(record -> record.getSubject().getId().equals(subject.getId()))
                .toList();
        long attended = countByStatus(subjectAttendance, AttendanceStatus.PRESENT);
        long missed = subjectAttendance.size() - attended;
        Double averageMarks = average(performanceMarks(subjectAttendance));
        return new AdminStudentProfile.SubjectPerformance(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                attended,
                missed,
                subjectAttendance.size(),
                percentage(attended, subjectAttendance.size()),
                averageMarks,
                null);
    }

    private AdminStudentProfile.AssignmentEntry toAssignmentEntry(
            Assignment assignment,
            AssignmentSubmission submission) {
        return new AdminStudentProfile.AssignmentEntry(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getSubject().getName(),
                assignment.getSubject().getCode(),
                assignment.getDueDate(),
                submission == null ? "PENDING" : submission.getStatus().name(),
                submission == null ? null : submission.getSubmittedAt(),
                submission == null ? null : submission.getMarks(),
                assignment.getMaxMarks());
    }

    private List<Integer> performanceMarks(List<Attendance> attendance) {
        return attendance.stream()
                .map(Attendance::getPerformanceMarks)
                .filter(Objects::nonNull)
                .toList();
    }

    private long countByStatus(List<Attendance> attendance, AttendanceStatus status) {
        return attendance.stream().filter(record -> record.getStatus() == status).count();
    }

    private Double percentage(long numerator, long denominator) {
        return denominator == 0 ? null : round(numerator * 100.0 / denominator);
    }

    private Double average(List<Integer> values) {
        return values.isEmpty()
                ? null
                : round(values.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    private boolean isPerformanceDeclining(List<Attendance> attendance) {
        List<Integer> marks = attendance.stream()
                .filter(record -> record.getPerformanceMarks() != null)
                .sorted(Comparator.comparing(Attendance::getDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Attendance::getPerformanceMarks)
                .toList();
        if (marks.size() < 4) {
            return false;
        }
        int midpoint = marks.size() / 2;
        double earlierAverage = marks.subList(0, midpoint).stream()
                .mapToInt(Integer::intValue).average().orElseThrow();
        double recentAverage = marks.subList(midpoint, marks.size()).stream()
                .mapToInt(Integer::intValue).average().orElseThrow();
        return earlierAverage - recentAverage >= 1.0;
    }

    private boolean matchesAttention(AdminStudentRosterItem student, String attention) {
        if (attention == null || attention.isBlank()) {
            return true;
        }
        return switch (attention.toUpperCase(Locale.ROOT)) {
            case "LOW_ATTENDANCE" -> student.lowAttendance();
            case "POOR_PERFORMANCE" -> student.poorPerformance();
            case "PENDING_ASSIGNMENTS" -> student.pendingAssignments() >= 2;
            case "DECLINING_PERFORMANCE" -> student.decliningPerformance();
            default -> throw new IllegalArgumentException("Unsupported attention filter.");
        };
    }

    private void validateAttentionFilter(String attention) {
        if (attention == null || attention.isBlank()) {
            return;
        }
        switch (attention.toUpperCase(Locale.ROOT)) {
            case "LOW_ATTENDANCE", "POOR_PERFORMANCE", "PENDING_ASSIGNMENTS", "DECLINING_PERFORMANCE" -> {
            }
            default -> throw new IllegalArgumentException("Unsupported attention filter.");
        }
    }

    private Comparator<AdminStudentRosterItem> rosterComparator(
            String sort,
            String defaultSortBy,
            String defaultDirection) {
        Comparator<AdminStudentRosterItem> comparator = null;
        for (SortCriterion criterion : parseSort(sort, defaultSortBy, defaultDirection)) {
            boolean descending = "desc".equals(criterion.direction());
            Comparator<AdminStudentRosterItem> next = switch (criterion.field()) {
                case "name" -> nullableComparator(AdminStudentRosterItem::name, descending, String.CASE_INSENSITIVE_ORDER);
                case "attendance" -> nullableComparator(AdminStudentRosterItem::attendancePercentage, descending, Comparator.naturalOrder());
                case "performance" -> nullableComparator(AdminStudentRosterItem::academicPerformance, descending, Comparator.naturalOrder());
                case "assignments" -> Comparator.comparingLong(AdminStudentRosterItem::pendingAssignments);
                case "submittedAssignments" -> Comparator.comparingLong(AdminStudentRosterItem::submittedAssignments);
                case "totalAssignments" -> Comparator.comparingLong(AdminStudentRosterItem::totalAssignments);
                case "year" -> nullableComparator(AdminStudentRosterItem::year, descending, Comparator.naturalOrder());
                case "semester" -> nullableComparator(AdminStudentRosterItem::semester, descending, Comparator.naturalOrder());
                case "yearSemester" -> nullableComparator(
                        (AdminStudentRosterItem item) -> item.year() == null && item.semester() == null
                                ? null
                                : (item.year() == null ? 0 : item.year()) * 100
                                + (item.semester() == null ? 0 : item.semester()),
                        descending,
                        Comparator.<Integer>naturalOrder());
                case "status" -> nullableComparator(item -> item.status() == null ? null : item.status().name(), descending, String.CASE_INSENSITIVE_ORDER);
                case "attention" -> Comparator.comparing(AdminStudentRosterItem::requiresAttention);
                case "rollNumber" -> nullableComparator(AdminStudentRosterItem::rollNumber, descending, String.CASE_INSENSITIVE_ORDER);
                default -> throw new IllegalArgumentException("Unsupported student sort field.");
            };
            if (descending && Set.of("assignments", "submittedAssignments", "totalAssignments", "attention").contains(criterion.field())) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator.thenComparing(
                AdminStudentRosterItem::rollNumber,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private List<SortCriterion> parseSort(String sort, String defaultSortBy, String defaultDirection) {
        if (sort == null || sort.isBlank()) {
            return List.of(new SortCriterion(defaultSortBy, defaultDirection.toLowerCase(Locale.ROOT)));
        }
        return Arrays.stream(sort.split(","))
                .map(value -> value.split(":", -1))
                .map(parts -> {
                    if (parts.length != 2 || !Set.of(
                            "rollNumber", "name", "attendance", "performance", "assignments",
                            "submittedAssignments", "totalAssignments", "year", "semester", "yearSemester", "status", "attention")
                            .contains(parts[0])) {
                        throw new IllegalArgumentException("Unsupported student sort field.");
                    }
                    String direction = parts[1].toLowerCase(Locale.ROOT);
                    if (!Set.of("asc", "desc").contains(direction)) {
                        throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
                    }
                    return new SortCriterion(parts[0], direction);
                })
                .toList();
    }

    private <T> Comparator<AdminStudentRosterItem> nullableComparator(
            Function<AdminStudentRosterItem, T> extractor,
            boolean descending,
            Comparator<T> order) {
        Comparator<T> selectedOrder = descending ? order.reversed() : order;
        return Comparator.comparing(extractor, Comparator.nullsLast(selectedOrder));
    }

    private record SortCriterion(String field, String direction) {}

    private void requireDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found.");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private void validateAcademicFilters(Integer year, Integer semester) {
        if (year != null && (year < 1 || year > 4)) {
            throw new IllegalArgumentException("Year must be between 1 and 4.");
        }
        if (semester != null && (semester < 1 || semester > 8)) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
        if (year != null && semester != null && year != (semester + 1) / 2) {
            throw new IllegalArgumentException("Semester must match the selected year.");
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record StudentData(
            User student,
            List<Subject> subjects,
            List<Attendance> attendance,
            List<Assignment> assignments,
            Map<Long, AssignmentSubmission> submissionsByAssignmentId) {
    }
}
