package com.institute.ims.attendance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(
            AttendanceService service) {

        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Attendance>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getByStudent(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                service.getByStudent(studentId)
        );
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<Attendance>> getBySubject(
            @PathVariable Long subjectId) {

        return ResponseEntity.ok(
                service.getBySubject(subjectId)
        );
    }

    @GetMapping("/percentage")
    public ResponseEntity<Double> getPercentage(
            @RequestParam Long studentId,
            @RequestParam Long subjectId) {

        return ResponseEntity.ok(
                service.getAttendancePercentage(
                        studentId,
                        subjectId
                )
        );
    }

    @PostMapping
    public ResponseEntity<Attendance> create(
            @RequestBody Map<String, String> body) {

        Long studentId =
                Long.parseLong(body.get("studentId"));

        Long subjectId =
                Long.parseLong(body.get("subjectId"));

        LocalDate date =
                LocalDate.parse(body.get("date"));

        AttendanceStatus status =
                AttendanceStatus.valueOf(
                        body.get("status")
                );

        return ResponseEntity.ok(
                service.create(
                        studentId,
                        subjectId,
                        date,
                        status
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attendance> update(
            @PathVariable Long id,
            @RequestParam AttendanceStatus status) {

        return ResponseEntity.ok(
                service.update(id, status)
        );
    }
}