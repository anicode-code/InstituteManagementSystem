package com.institute.ims.controller;

import com.institute.ims.model.Subject;
import com.institute.ims.service.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Subject>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<Subject>> getByFaculty(
            @PathVariable Long facultyId) {

        return ResponseEntity.ok(
                service.getByFaculty(facultyId)
        );
    }

    @PostMapping
    public ResponseEntity<Subject> create(
            @RequestBody Map<String, Object> body) {

        String name = (String) body.get("name");
        String code = (String) body.get("code");

        Integer credits = body.get("credits") == null
                ? null
                : ((Number) body.get("credits")).intValue();

        Long departmentId =
                ((Number) body.get("departmentId")).longValue();

        Long facultyId =
                ((Number) body.get("facultyId")).longValue();

        return ResponseEntity.ok(
                service.create(
                        name,
                        code,
                        credits,
                        departmentId,
                        facultyId
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subject> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String name = (String) body.get("name");
        String code = (String) body.get("code");

        Integer credits = body.get("credits") == null
                ? null
                : ((Number) body.get("credits")).intValue();

        return ResponseEntity.ok(
                service.update(
                        id,
                        name,
                        code,
                        credits
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Subject deleted successfully"
        );
    }
}
