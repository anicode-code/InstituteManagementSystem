package com.institute.ims.service;

import com.institute.ims.common.ResourceNotFoundException;
import com.institute.ims.dto.AdminHodDirectory;
import com.institute.ims.dto.AdminHodDirectoryItem;
import com.institute.ims.dto.AdminHodProfile;
import com.institute.ims.model.*;
import com.institute.ims.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDepartmentHodService {

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

    public AdminDepartmentHodService(
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
    public AdminHodDirectory getDirectory(Long departmentId) {
        Department department = getDepartment(departmentId);
        User hod = department.getHod();
        int studentCount = userRepository.findByRoleAndDepartmentId(Role.STUDENT, departmentId).size();
        List<Subject> subjects = subjectRepository.findByDepartmentIdWithFacultyMembers(departmentId);
        List<User> faculty = getDepartmentFaculty(departmentId, subjects);
        AdminHodDirectoryItem currentHod = hod == null
                ? null
                : toDirectoryItem(hod, department);
        return new AdminHodDirectory(
                currentHod,
                new AdminHodDirectory.Summary(faculty.size(), studentCount, subjects.size()));
    }

    @Transactional(readOnly = true)
    public AdminHodProfile getProfile(Long departmentId, Long hodId) {
        Department department = getDepartment(departmentId);
        User hod = department.getHod();
        if (hod == null || !Objects.equals(hod.getId(), hodId)) {
            if (userRepository.existsById(hodId)) {
                throw new AccessDeniedException("You may only view the current HOD of this department.");
            }
            throw new ResourceNotFoundException("HOD not found.");
        }

        DepartmentData data = loadDepartmentData(department);
        List<AdminHodProfile.FacultyMember> facultyMembers = data.faculty().stream()
                .map(faculty -> toFacultyMember(faculty, data.subjects()))
                .toList();
        List<AdminHodProfile.SubjectDetails> subjectDetails = data.subjects().stream()
                .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .map(subject -> toSubjectDetails(subject, data))
                .toList();
        List<AdminHodProfile.StudentAttention> studentsRequiringAttention =
                studentsRequiringAttention(data.attendance());
        List<AdminHodProfile.SubjectAttendance> subjectAttendance = data.subjects().stream()
                .map(subject -> toSubjectAttendance(subject, data))
                .toList();
        List<AdminHodProfile.FacultyAttendance> facultyAttendance = data.faculty().stream()
                .map(faculty -> toFacultyAttendance(faculty, data))
                .toList();

        AdminHodProfile.StudentOverview studentOverview = new AdminHodProfile.StudentOverview(
                data.students().size(),
                distribution(data.students().stream().map(User::getYear).toList()),
                distribution(data.students().stream().map(User::getSemester).toList()),
                attendancePercentage(data.attendance()),
                averagePerformance(data.attendance()),
                studentsRequiringAttention);
        AdminHodProfile.AttendanceOverview attendanceOverview = new AdminHodProfile.AttendanceOverview(
                attendancePercentage(data.attendance()),
                subjectAttendance,
                facultyAttendance,
                studentsRequiringAttention.stream()
                        .filter(student -> student.reasons().contains("LOW ATTENDANCE"))
                        .toList(),
                attendanceTrends(data.attendance()));
        AdminHodProfile.AssignmentOverview assignmentOverview = assignmentOverview(data);
        AdminHodProfile.AcademicPerformanceOverview academicPerformanceOverview =
                academicPerformanceOverview(data, studentsRequiringAttention);

        return new AdminHodProfile(
                personalInformation(hod),
                professionalInformation(department),
                new AdminHodProfile.DepartmentOverview(
                        data.students().size(),
                        data.faculty().size(),
                        data.subjects().size(),
                        assignmentOverview.totalActiveAssignments(),
                        attendancePercentage(data.attendance()),
                        averagePerformance(data.attendance())),
                facultyMembers,
                studentOverview,
                subjectDetails,
                attendanceOverview,
                assignmentOverview,
                academicPerformanceOverview,
                new AdminHodProfile.AdministrativeInformation(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));
    }

    private DepartmentData loadDepartmentData(Department department) {
        Long departmentId = department.getId();
        List<User> students = getStudents(departmentId);
        List<Long> studentIds = students.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();
        List<User> activeStudents = students.stream()
                .filter(student -> student.getStatus() == VerificationStatus.VERIFIED)
                .toList();

        List<Subject> subjects = subjectRepository.findByDepartmentIdWithFacultyMembers(departmentId);
        List<User> faculty = getDepartmentFaculty(departmentId, subjects);
        Map<Long, Set<Long>> enrolledStudentIdsBySubject = enrollmentBySubject(
                subjects, activeStudents, departmentId);
        Set<Long> relevantStudentIds = new HashSet<>(studentIds);
        Set<Long> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toSet());
        Map<Long, List<Attendance>> attendanceBySubject = new HashMap<>();
        subjects.forEach(subject -> attendanceBySubject.put(subject.getId(), new ArrayList<>()));
        if (!subjectIds.isEmpty()) {
            attendanceRepository.findBySubjectIdIn(new ArrayList<>(subjectIds)).stream()
                    .filter(record -> record.getStudent() != null
                            && record.getSubject() != null
                            && relevantStudentIds.contains(record.getStudent().getId()))
                    .forEach(record -> attendanceBySubject
                            .computeIfAbsent(record.getSubject().getId(), ignored -> new ArrayList<>())
                            .add(record));
        }
        List<Attendance> attendance = attendanceBySubject.values().stream()
                .flatMap(Collection::stream)
                .toList();

        List<Assignment> assignments = subjectIds.isEmpty()
                ? List.of()
                : assignmentRepository.findBySubjectIdIn(new ArrayList<>(subjectIds));
        Set<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toSet());
        List<AssignmentSubmission> submissions = assignmentIds.isEmpty()
                ? List.of()
                : submissionRepository.findByAssignmentIdIn(new ArrayList<>(assignmentIds));

        return new DepartmentData(
                students,
                faculty,
                subjects,
                enrolledStudentIdsBySubject,
                attendance,
                attendanceBySubject,
                assignments,
                submissions,
                relevantStudentIds);
    }

    private List<User> getStudents(Long departmentId) {
        List<User> students = userRepository.findByRoleAndDepartmentId(Role.STUDENT, departmentId);
        List<Long> ids = students.stream().map(User::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findWithSelectedSubjectsByIdIn(ids);
    }

    private List<User> getDepartmentFaculty(Long departmentId, List<Subject> subjects) {
        Map<Long, User> faculty = new LinkedHashMap<>();
        userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, departmentId)
                .forEach(member -> faculty.put(member.getId(), member));
        subjects.stream()
                .flatMap(subject -> subjectFacultyMembers(subject).stream())
                .filter(member -> member.getRole() == Role.FACULTY || member.getRole() == Role.HOD)
                .forEach(member -> faculty.putIfAbsent(member.getId(), member));
        return faculty.values().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<Long, Set<Long>> enrollmentBySubject(
            List<Subject> subjects,
            List<User> activeStudents,
            Long departmentId) {
        Map<Long, Set<Long>> enrollments = new HashMap<>();
        for (Subject subject : subjects) {
            Set<Long> studentIds = activeStudents.stream()
                    .filter(student -> isEnrolledIn(student, subject, departmentId))
                    .map(User::getId)
                    .collect(Collectors.toSet());
            enrollments.put(subject.getId(), studentIds);
        }
        return enrollments;
    }

    private boolean isEnrolledIn(User student, Subject subject, Long departmentId) {
        boolean selected = student.getSelectedSubjects() != null
                && student.getSelectedSubjects().stream()
                        .anyMatch(item -> Objects.equals(item.getId(), subject.getId()));
        boolean compulsory = subject.getType() == SubjectType.COMPULSORY
                && student.getDepartment() != null
                && Objects.equals(student.getDepartment().getId(), departmentId)
                && Objects.equals(student.getSemester(), subject.getSemester());
        return selected || compulsory;
    }

    private AdminHodProfile.PersonalInformation personalInformation(User hod) {
        return new AdminHodProfile.PersonalInformation(
                hod.getName(),
                null,
                hod.getRollNumber(),
                hod.getEmail(),
                null,
                null,
                null,
                null,
                null,
                null,
                hod.getCreatedAt(),
                hod.getStatus(),
                null);
    }

    private AdminHodProfile.ProfessionalInformation professionalInformation(Department department) {
        return new AdminHodProfile.ProfessionalInformation(
                department.getName(),
                department.getCode(),
                null,
                null,
                null,
                null,
                null,
                List.of());
    }

    private AdminHodProfile.FacultyMember toFacultyMember(User faculty, List<Subject> subjects) {
        List<Subject> assignedSubjects = subjects.stream()
                .filter(subject -> subjectFacultyMembers(subject).stream()
                        .anyMatch(member -> Objects.equals(member.getId(), faculty.getId())))
                .toList();
        return new AdminHodProfile.FacultyMember(
                faculty.getId(),
                faculty.getName(),
                faculty.getRollNumber(),
                null,
                assignedSubjects.stream()
                        .map(subject -> subject.getName() + " (" + subject.getCode() + ")")
                        .toList(),
                assignedSubjects.stream()
                        .map(Subject::getAcademicYear)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList(),
                assignedSubjects.stream()
                        .map(Subject::getSemester)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList(),
                faculty.getStatus());
    }

    private AdminHodProfile.SubjectDetails toSubjectDetails(Subject subject, DepartmentData data) {
        return new AdminHodProfile.SubjectDetails(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                subject.getAcademicYear(),
                subject.getSemester(),
                subject.getAcademicYear(),
                data.enrolledStudentIdsBySubject().getOrDefault(subject.getId(), Set.of()).size(),
                subjectFacultyMembers(subject).stream()
                        .map(User::getName)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList());
    }

    private AdminHodProfile.SubjectAttendance toSubjectAttendance(Subject subject, DepartmentData data) {
        List<Attendance> records = data.attendanceBySubject()
                .getOrDefault(subject.getId(), List.of());
        return new AdminHodProfile.SubjectAttendance(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                records.size(),
                attendancePercentage(records));
    }

    private AdminHodProfile.FacultyAttendance toFacultyAttendance(User faculty, DepartmentData data) {
        Set<Long> subjectIds = data.subjects().stream()
                .filter(subject -> subjectFacultyMembers(subject).stream()
                        .anyMatch(member -> Objects.equals(member.getId(), faculty.getId())))
                .map(Subject::getId)
                .collect(Collectors.toSet());
        List<Attendance> records = subjectIds.stream()
                .flatMap(subjectId -> data.attendanceBySubject()
                        .getOrDefault(subjectId, List.of()).stream())
                .toList();
        return new AdminHodProfile.FacultyAttendance(
                faculty.getId(),
                faculty.getName(),
                faculty.getRollNumber(),
                records.size(),
                attendancePercentage(records));
    }

    private List<AdminHodProfile.StudentAttention> studentsRequiringAttention(List<Attendance> attendance) {
        return attendance.stream()
                .filter(record -> record.getStudent() != null)
                .collect(Collectors.groupingBy(record -> record.getStudent().getId()))
                .values().stream()
                .map(this::toStudentAttention)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        AdminHodProfile.StudentAttention::name,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AdminHodProfile.StudentAttention toStudentAttention(List<Attendance> records) {
        Double attendance = attendancePercentage(records);
        Double performance = averagePerformance(records);
        List<String> reasons = new ArrayList<>();
        if (attendance != null && attendance < attendanceThreshold) {
            reasons.add("LOW ATTENDANCE");
        }
        if (performance != null && performance < performanceThreshold) {
            reasons.add("POOR PERFORMANCE");
        }
        if (reasons.isEmpty()) {
            return null;
        }
        User student = records.get(0).getStudent();
        return new AdminHodProfile.StudentAttention(
                student.getId(),
                student.getName(),
                student.getRollNumber(),
                attendance,
                performance,
                reasons);
    }

    private List<AdminHodProfile.AttendanceTrend> attendanceTrends(List<Attendance> attendance) {
        Map<YearMonth, List<Attendance>> byMonth = attendance.stream()
                .filter(record -> record.getDate() != null)
                .collect(Collectors.groupingBy(
                        record -> YearMonth.from(record.getDate()),
                        TreeMap::new,
                        Collectors.toList()));
        return byMonth.entrySet().stream()
                .map(entry -> new AdminHodProfile.AttendanceTrend(
                        entry.getKey().toString(),
                        entry.getValue().size(),
                        attendancePercentage(entry.getValue())))
                .toList();
    }

    private AdminHodProfile.AssignmentOverview assignmentOverview(DepartmentData data) {
        LocalDate today = LocalDate.now();
        Map<Long, List<AssignmentSubmission>> submissionsByAssignment = data.submissions().stream()
                .collect(Collectors.groupingBy(submission -> submission.getAssignment().getId()));
        Map<Long, User> facultyById = data.faculty().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, ignored) -> first));
        data.assignments().stream()
                .map(Assignment::getFaculty)
                .filter(Objects::nonNull)
                .forEach(faculty -> facultyById.putIfAbsent(faculty.getId(), faculty));
        int activeAssignments = 0;
        int overdueAssignments = 0;
        int pendingSubmissions = 0;
        int assignmentsAwaitingGrading = 0;
        Map<Long, Integer> assignmentCountsByFaculty = new HashMap<>();
        List<AdminHodProfile.AssignmentDetails> details = new ArrayList<>();
        for (Assignment assignment : data.assignments()) {
            boolean active = assignment.getDueDate() != null && !assignment.getDueDate().isBefore(today);
            if (active) {
                activeAssignments++;
            } else if (assignment.getDueDate() != null && assignment.getDueDate().isBefore(today)) {
                overdueAssignments++;
            }
            List<AssignmentSubmission> submissions = submissionsByAssignment
                    .getOrDefault(assignment.getId(), List.of()).stream()
                    .filter(submission -> submission.getStudent() != null
                            && data.departmentStudentIds().contains(submission.getStudent().getId()))
                    .toList();
            Set<Long> enrolledStudentIds = data.enrolledStudentIdsBySubject()
                    .getOrDefault(assignment.getSubject().getId(), Set.of());
            Set<Long> submittedStudentIds = submissions.stream()
                    .map(submission -> submission.getStudent().getId())
                    .filter(enrolledStudentIds::contains)
                    .collect(Collectors.toSet());
            int pending = Math.max(0, enrolledStudentIds.size() - submittedStudentIds.size());
            pendingSubmissions += pending;
            long evaluated = submissions.stream()
                    .filter(submission -> submission.getStatus() == SubmissionStatus.EVALUATED)
                    .map(submission -> submission.getStudent().getId())
                    .distinct()
                    .count();
            boolean awaitingGrading = !submissions.isEmpty() && evaluated < submissions.size();
            if (awaitingGrading) {
                assignmentsAwaitingGrading++;
            }
            User creator = assignment.getFaculty();
            if (creator != null) {
                assignmentCountsByFaculty.merge(creator.getId(), 1, Integer::sum);
            }
            details.add(new AdminHodProfile.AssignmentDetails(
                    assignment.getId(),
                    assignment.getTitle(),
                    assignment.getSubject().getName(),
                    assignment.getSubject().getCode(),
                    creator == null ? null : creator.getName(),
                    null,
                    assignment.getDueDate(),
                    submissions.size(),
                    pending,
                    gradingStatus(submissions, evaluated),
                    active));
        }

        List<AdminHodProfile.FacultyAssignmentCount> facultyDistribution = assignmentCountsByFaculty.entrySet()
                .stream()
                .map(entry -> {
                    User faculty = facultyById.get(entry.getKey());
                    return new AdminHodProfile.FacultyAssignmentCount(
                            entry.getKey(),
                            faculty == null ? "N/A" : faculty.getName(),
                            faculty == null ? null : faculty.getRollNumber(),
                            entry.getValue());
                })
                .sorted(Comparator.comparing(
                        AdminHodProfile.FacultyAssignmentCount::facultyName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        details.sort(Comparator.comparing(AdminHodProfile.AssignmentDetails::dueDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return new AdminHodProfile.AssignmentOverview(
                data.assignments().size(),
                activeAssignments,
                overdueAssignments,
                pendingSubmissions,
                assignmentsAwaitingGrading,
                facultyDistribution,
                details);
    }

    private String gradingStatus(List<AssignmentSubmission> submissions, long evaluated) {
        if (submissions.isEmpty()) {
            return "NO SUBMISSIONS";
        }
        if (evaluated == submissions.size()) {
            return "GRADED";
        }
        return evaluated == 0 ? "PENDING GRADING" : "PARTIALLY GRADED";
    }

    private AdminHodProfile.AcademicPerformanceOverview academicPerformanceOverview(
            DepartmentData data,
            List<AdminHodProfile.StudentAttention> attention) {
        List<AdminHodProfile.SubjectPerformance> subjectPerformance = data.subjects().stream()
                .map(subject -> {
                    List<Attendance> records = data.attendanceBySubject()
                            .getOrDefault(subject.getId(), List.of());
                    int assessedRecords = (int) records.stream()
                            .filter(record -> record.getPerformanceMarks() != null)
                            .count();
                    return new AdminHodProfile.SubjectPerformance(
                            subject.getId(),
                            subject.getName(),
                            subject.getCode(),
                            averagePerformance(records),
                            assessedRecords);
                })
                .toList();
        return new AdminHodProfile.AcademicPerformanceOverview(
                averagePerformance(data.attendance()),
                performanceGroups(data.attendance(), User::getYear),
                performanceGroups(data.attendance(), User::getSemester),
                subjectPerformance,
                null,
                attention);
    }

    private List<AdminHodProfile.PerformanceGroup> performanceGroups(
            List<Attendance> attendance,
            Function<User, Integer> groupingKey) {
        Map<Integer, List<Integer>> marksByGroup = new TreeMap<>();
        for (Attendance record : attendance) {
            Integer group = record.getStudent() == null ? null : groupingKey.apply(record.getStudent());
            if (group == null || record.getPerformanceMarks() == null) {
                continue;
            }
            marksByGroup.computeIfAbsent(group, ignored -> new ArrayList<>())
                    .add(record.getPerformanceMarks());
        }
        return marksByGroup.entrySet().stream()
                .map(entry -> new AdminHodProfile.PerformanceGroup(
                        entry.getKey(),
                        average(entry.getValue()),
                        entry.getValue().size()))
                .toList();
    }

    private List<AdminHodProfile.Distribution> distribution(List<Integer> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new AdminHodProfile.Distribution(
                        entry.getKey(),
                        Math.toIntExact(entry.getValue())))
                .toList();
    }

    private List<User> subjectFacultyMembers(Subject subject) {
        if (subject.getFacultyMembers() != null && !subject.getFacultyMembers().isEmpty()) {
            return new ArrayList<>(subject.getFacultyMembers());
        }
        User legacyFaculty = subject.getFaculty();
        return legacyFaculty == null ? List.of() : List.of(legacyFaculty);
    }

    private AdminHodDirectoryItem toDirectoryItem(User hod, Department department) {
        return new AdminHodDirectoryItem(
                hod.getId(),
                hod.getRollNumber(),
                hod.getName(),
                null,
                null,
                hod.getEmail(),
                null,
                department.getName(),
                department.getCode(),
                null,
                hod.getStatus());
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
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
        return marks.isEmpty() ? null : average(marks);
    }

    private Double average(List<Integer> values) {
        return values.isEmpty()
                ? null
                : round(values.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    private Double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record DepartmentData(
            List<User> students,
            List<User> faculty,
            List<Subject> subjects,
            Map<Long, Set<Long>> enrolledStudentIdsBySubject,
            List<Attendance> attendance,
            Map<Long, List<Attendance>> attendanceBySubject,
            List<Assignment> assignments,
            List<AssignmentSubmission> submissions,
            Set<Long> departmentStudentIds) {
    }
}
