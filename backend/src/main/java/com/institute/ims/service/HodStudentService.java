package com.institute.ims.service;

import com.institute.ims.dto.HodStudentDetails;
import com.institute.ims.dto.HodStudentRosterItem;
import com.institute.ims.model.*;
import com.institute.ims.repository.AssignmentRepository;
import com.institute.ims.repository.AssignmentSubmissionRepository;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

@Service
public class HodStudentService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectService subjectService;
    private final AttendanceService attendanceService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public HodStudentService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            SubjectService subjectService,
            AttendanceService attendanceService,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.subjectService = subjectService;
        this.attendanceService = attendanceService;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public List<HodStudentRosterItem> getRoster(
            String hodRollNumber,
            Integer year,
            String sortBy,
            String direction) {
        if (year == null || year < 1 || year > 4) {
            throw new IllegalArgumentException("Academic year must be between 1 and 4.");
        }

        User hod = getHod(hodRollNumber);
        List<Department> departments = getAssignedDepartments(hod);
        Set<Long> departmentIds = departmentIds(departments);
        List<User> students = userRepository
                .findByRoleAndYearAndDepartmentIdInOrderByRollNumberAsc(
                        Role.STUDENT, year, new ArrayList<>(departmentIds));

        List<HodStudentRosterItem> roster = students.stream()
                .map(student -> getStudentData(student, departmentIds).rosterItem())
                .toList();

        Comparator<HodStudentRosterItem> comparator = rosterComparator(sortBy, direction);
        return roster.stream()
                .sorted(comparator.thenComparing(HodStudentRosterItem::rollNumber))
                .toList();
    }

    @Transactional(readOnly = true)
    public HodStudentDetails getStudentDetails(String hodRollNumber, Long studentId) {
        User hod = getHod(hodRollNumber);
        Set<Long> departmentIds = departmentIds(getAssignedDepartments(hod));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new NoSuchElementException("Student not found."));

        if (student.getRole() != Role.STUDENT
                || student.getDepartment() == null
                || !departmentIds.contains(student.getDepartment().getId())) {
            throw new AccessDeniedException("You may only view students in your assigned department.");
        }

        StudentData data = getStudentData(student, departmentIds);
        return new HodStudentDetails(
                data.rosterItem(),
                data.subjects().stream()
                        .map(subject -> new HodStudentDetails.SubjectSummary(
                                subject.getId(),
                                subject.getName(),
                                subject.getCode(),
                                subject.getSemester(),
                                subject.getAcademicYear()))
                        .toList(),
                data.attendance().stream()
                        .map(record -> new HodStudentDetails.AttendanceDetail(
                                record.getId(),
                                record.getSubject().getName(),
                                record.getSubject().getCode(),
                                record.getDate(),
                                record.getStatus(),
                                record.getPerformanceMarks()))
                        .toList(),
                data.assignments().stream()
                        .map(assignment -> toAssignmentDetail(assignment, data.submissionsByAssignmentId()))
                        .toList());
    }

    private StudentData getStudentData(User student, Set<Long> departmentIds) {
        List<Subject> subjects = subjectService.getByStudent(student.getId()).stream()
                .filter(subject -> subject.getDepartment() != null
                        && departmentIds.contains(subject.getDepartment().getId()))
                .toList();
        Set<Long> subjectIds = subjects.stream().map(Subject::getId).collect(java.util.stream.Collectors.toSet());

        List<Attendance> attendance = attendanceService.getByStudent(student.getId()).stream()
                .filter(record -> record.getSubject() != null && subjectIds.contains(record.getSubject().getId()))
                .toList();
        Double attendancePercentage = attendance.isEmpty()
                ? null
                : roundToOneDecimal(attendance.stream()
                        .filter(record -> record.getStatus() == AttendanceStatus.PRESENT)
                        .count() * 100.0 / attendance.size());
        List<Integer> marks = attendance.stream()
                .map(Attendance::getPerformanceMarks)
                .filter(Objects::nonNull)
                .toList();
        Double classPerformance = marks.isEmpty()
                ? null
                : roundToOneDecimal(marks.stream().mapToInt(Integer::intValue).average().orElseThrow());

        List<Assignment> assignments = subjectIds.isEmpty()
                ? List.of()
                : assignmentRepository.findBySubjectIdIn(new ArrayList<>(subjectIds));
        Set<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, AssignmentSubmission> submissionsByAssignmentId = submissionRepository
                .findByStudentId(student.getId()).stream()
                .filter(submission -> assignmentIds.contains(submission.getAssignment().getId()))
                .collect(java.util.stream.Collectors.toMap(
                        submission -> submission.getAssignment().getId(),
                        Function.identity(),
                        (first, ignored) -> first));
        long pendingAssignments = assignments.stream()
                .filter(assignment -> !submissionsByAssignmentId.containsKey(assignment.getId()))
                .count();

        Department department = student.getDepartment();
        HodStudentRosterItem item = new HodStudentRosterItem(
                student.getId(),
                student.getName(),
                student.getRollNumber(),
                student.getEmail(),
                student.getStatus(),
                student.getYear(),
                student.getSemester(),
                student.getCreatedAt(),
                department == null ? null : new HodStudentRosterItem.DepartmentSummary(
                        department.getId(), department.getName(), department.getCode()),
                attendancePercentage,
                classPerformance,
                pendingAssignments);

        return new StudentData(item, subjects, attendance, assignments, submissionsByAssignmentId);
    }

    private HodStudentDetails.AssignmentDetail toAssignmentDetail(
            Assignment assignment,
            Map<Long, AssignmentSubmission> submissionsByAssignmentId) {
        AssignmentSubmission submission = submissionsByAssignmentId.get(assignment.getId());
        return new HodStudentDetails.AssignmentDetail(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getSubject().getName(),
                assignment.getSubject().getCode(),
                assignment.getDueDate(),
                assignment.getMaxMarks(),
                submission != null,
                submission == null ? null : submission.getSubmissionText(),
                submission == null ? null : submission.getSubmittedAt(),
                submission == null ? "NOT_SUBMITTED" : submission.getStatus().name(),
                submission == null ? null : submission.getMarks(),
                submission == null ? null : submission.getFeedback());
    }

    private User getHod(String rollNumber) {
        User user = userRepository.findByRollNumber(rollNumber)
                .orElseThrow(() -> new AccessDeniedException("HOD account not found."));
        if (user.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only HODs may access the student roster.");
        }
        return user;
    }

    private List<Department> getAssignedDepartments(User hod) {
        List<Department> departments = departmentRepository.findByHodId(hod.getId());
        if (departments.isEmpty()) {
            throw new AccessDeniedException("No department is assigned to this HOD account.");
        }
        return departments;
    }

    private Set<Long> departmentIds(List<Department> departments) {
        return departments.stream().map(Department::getId).collect(java.util.stream.Collectors.toSet());
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Comparator<HodStudentRosterItem> rosterComparator(String sortBy, String direction) {
        boolean descending;
        if ("asc".equalsIgnoreCase(direction)) {
            descending = false;
        } else if ("desc".equalsIgnoreCase(direction)) {
            descending = true;
        } else {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }

        return switch (sortBy) {
            case "rollNumber" -> metricComparator(HodStudentRosterItem::rollNumber, Comparator.naturalOrder(), descending);
            case "name" -> metricComparator(HodStudentRosterItem::name, String.CASE_INSENSITIVE_ORDER, descending);
            case "department" -> metricComparator(
                    item -> item.department() == null ? null : item.department().name(),
                    String.CASE_INSENSITIVE_ORDER,
                    descending);
            case "attendancePercentage" -> metricComparator(HodStudentRosterItem::attendancePercentage, Comparator.naturalOrder(), descending);
            case "classPerformance" -> metricComparator(HodStudentRosterItem::classPerformance, Comparator.naturalOrder(), descending);
            case "pendingAssignments" -> metricComparator(HodStudentRosterItem::pendingAssignments, Comparator.naturalOrder(), descending);
            default -> throw new IllegalArgumentException("Unsupported roster sort column.");
        };
    }

    private <T> Comparator<HodStudentRosterItem> metricComparator(
            Function<HodStudentRosterItem, T> keyExtractor,
            Comparator<T> valueComparator,
            boolean descending) {
        Comparator<T> orderedValues = descending ? valueComparator.reversed() : valueComparator;
        return Comparator.comparing(keyExtractor, Comparator.nullsLast(orderedValues));
    }

    private record StudentData(
            HodStudentRosterItem rosterItem,
            List<Subject> subjects,
            List<Attendance> attendance,
            List<Assignment> assignments,
            Map<Long, AssignmentSubmission> submissionsByAssignmentId) {
    }
}
