package com.institute.ims.service;

import com.institute.ims.model.*;
import com.institute.ims.repository.AttendanceRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository) {

        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    private void validateFacultySubjectAccess(Subject subject, User requestingUser) {
        if (requestingUser.getRole() == Role.FACULTY) {
            if (subject.getFaculty() == null || !subject.getFaculty().getId().equals(requestingUser.getId())) {
                throw new RuntimeException("You can only manage attendance for your assigned subjects.");
            }
        } else if (requestingUser.getRole() == Role.HOD) {
            if (requestingUser.getDepartment() != null && subject.getDepartment() != null &&
                    !subject.getDepartment().getId().equals(requestingUser.getDepartment().getId())) {
                throw new RuntimeException("You can only manage attendance within your department.");
            }
        } else if (requestingUser.getRole() == Role.STUDENT) {
            throw new RuntimeException("Students are not permitted to mark or modify attendance.");
        }
    }

    public Attendance createOrUpdate(
            Long studentId,
            Long subjectId,
            LocalDate date,
            AttendanceStatus status,
            Integer performanceMarks,
            User requestingUser) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("Attendance can only be marked for students.");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        validateFacultySubjectAccess(subject, requestingUser);

        // Prevent duplicate attendance records: update if already exists
        Optional<Attendance> existing = attendanceRepository.findByStudentIdAndSubjectIdAndDate(studentId, subjectId, date);
        if (existing.isPresent()) {
            Attendance attendance = existing.get();
            attendance.setStatus(status);
            if (performanceMarks != null) {
                attendance.setPerformanceMarks(performanceMarks);
            }
            return attendanceRepository.save(attendance);
        }

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(date);
        attendance.setStatus(status);
        attendance.setPerformanceMarks(performanceMarks);

        return attendanceRepository.save(attendance);
    }

    public Attendance create(
            Long studentId,
            Long subjectId,
            LocalDate date,
            AttendanceStatus status,
            User requestingUser) {
        return createOrUpdate(studentId, subjectId, date, status, null, requestingUser);
    }

    @Transactional
    public List<Attendance> recordDayBatch(
            Long subjectId,
            LocalDate date,
            List<Map<String, Object>> entries,
            User requestingUser) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        validateFacultySubjectAccess(subject, requestingUser);

        List<Attendance> saved = new ArrayList<>();

        for (Map<String, Object> entry : entries) {
            Long studentId = Long.parseLong(entry.get("studentId").toString());
            AttendanceStatus status = AttendanceStatus.valueOf(entry.get("status").toString());
            Integer perf = entry.get("performanceMarks") != null && !entry.get("performanceMarks").toString().isEmpty()
                    ? Integer.parseInt(entry.get("performanceMarks").toString())
                    : null;

            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

            Optional<Attendance> existing = attendanceRepository.findByStudentIdAndSubjectIdAndDate(studentId, subjectId, date);
            Attendance att = existing.orElseGet(Attendance::new);
            att.setStudent(student);
            att.setSubject(subject);
            att.setDate(date);
            att.setStatus(status);
            if (perf != null) {
                att.setPerformanceMarks(perf);
            }
            saved.add(attendanceRepository.save(att));
        }

        return saved;
    }

    @Transactional
    public void deleteDay(Long subjectId, LocalDate date, User requestingUser) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        validateFacultySubjectAccess(subject, requestingUser);

        attendanceRepository.deleteBySubjectIdAndDate(subjectId, date);
    }

    public List<Attendance> getByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public List<Attendance> getBySubject(Long subjectId) {
        return attendanceRepository.findBySubjectId(subjectId);
    }

    public List<Attendance> getAll() {
        return attendanceRepository.findAll();
    }

    public List<Attendance> getScopedAttendance(User user) {
        if (user.getRole() == Role.STUDENT) {
            return attendanceRepository.findByStudentId(user.getId());
        } else if (user.getRole() == Role.FACULTY) {
            return attendanceRepository.findBySubjectFacultyId(user.getId());
        } else if (user.getRole() == Role.HOD && user.getDepartment() != null) {
            return attendanceRepository.findBySubjectDepartmentId(user.getDepartment().getId());
        }
        return attendanceRepository.findAll();
    }

    public Attendance update(
            Long id,
            AttendanceStatus status,
            User requestingUser) {
        return update(id, status, null, requestingUser);
    }

    public Attendance update(
            Long id,
            AttendanceStatus status,
            Integer performanceMarks,
            User requestingUser) {

        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        validateFacultySubjectAccess(attendance.getSubject(), requestingUser);

        attendance.setStatus(status);
        if (performanceMarks != null) {
            attendance.setPerformanceMarks(performanceMarks);
        }
        return attendanceRepository.save(attendance);
    }

    public double getAttendancePercentage(
            Long studentId,
            Long subjectId) {

        List<Attendance> records = (subjectId != null)
                ? attendanceRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                : attendanceRepository.findByStudentId(studentId);

        if (records.isEmpty()) {
            return 0.0;
        }

        double attended = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();

        double percentage = (attended * 100.0) / records.size();
        return Math.round(percentage * 10.0) / 10.0;
    }
}
