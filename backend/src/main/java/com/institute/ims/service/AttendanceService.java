package com.institute.ims.service;

import com.institute.ims.model.Attendance;
import com.institute.ims.model.AttendanceStatus;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.repository.AttendanceRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

    public Attendance create(
            Long studentId,
            Long subjectId,
            LocalDate date,
            AttendanceStatus status) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Subject not found"));

        Attendance attendance = new Attendance();

        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(date);
        attendance.setStatus(status);

        return attendanceRepository.save(attendance);
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

    public Attendance update(
            Long id,
            AttendanceStatus status) {

        Attendance attendance =
                attendanceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance not found"));

        attendance.setStatus(status);

        return attendanceRepository.save(attendance);
    }

    public double getAttendancePercentage(
            Long studentId,
            Long subjectId) {

        List<Attendance> records =
                attendanceRepository
                        .findByStudentIdAndSubjectId(
                                studentId,
                                subjectId
                        );

        if (records.isEmpty()) {
            return 0.0;
        }

        long present = records.stream()
                .filter(a ->
                        a.getStatus()
                                == AttendanceStatus.PRESENT)
                .count();

        return (present * 100.0) / records.size();
    }
}
