package com.institute.ims.controller;

import com.institute.ims.model.Assignment;
import com.institute.ims.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(
            AssignmentService service) {

        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Assignment>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assignment> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<Assignment>> getByFaculty(
            @PathVariable Long facultyId) {

        return ResponseEntity.ok(
                service.getByFaculty(facultyId)
        );
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<Assignment>> getBySubject(
            @PathVariable Long subjectId) {

        return ResponseEntity.ok(
                service.getBySubject(subjectId)
        );
    }

    @PostMapping
    public ResponseEntity<Assignment> create(
            @RequestBody Map<String, String> body) {

        String title = body.get("title");
        String description = body.get("description");

        LocalDate dueDate =
                LocalDate.parse(body.get("dueDate"));

        Long subjectId =
                Long.parseLong(body.get("subjectId"));

        Long facultyId =
                Long.parseLong(body.get("facultyId"));

        return ResponseEntity.ok(
                service.create(
                        title,
                        description,
                        dueDate,
                        subjectId,
                        facultyId
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Assignment deleted successfully"
        );
    }
}
