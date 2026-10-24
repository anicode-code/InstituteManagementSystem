package com.institute.ims.controller;

import com.institute.ims.model.Department;
import com.institute.ims.model.User;
import com.institute.ims.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(
            DepartmentService service) {

        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> create(
            @RequestBody Department department) {

        return ResponseEntity.ok(
                service.create(department)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> update(
            @PathVariable Long id,
            @RequestBody Department department) {

        return ResponseEntity.ok(
                service.update(id, department)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Department deleted successfully"
        );
    }

    // ─── HOD Management ───────────────────────────────────────────────

    /**
     * Get list of verified faculty eligible to be HOD of a department.
     * A faculty member is eligible if they registered with this department.
     */
    @GetMapping("/{id}/eligible-hods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getEligibleHods(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getEligibleHods(id)
        );
    }

    /**
     * Assign or replace the HOD of a department.
     * Body: { "facultyId": 123 }
     */
    @PutMapping("/{id}/hod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> assignHod(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {

        Long facultyId = body.get("facultyId");

        if (facultyId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                service.assignHod(id, facultyId)
        );
    }

    /**
     * Remove the HOD from a department.
     */
    @DeleteMapping("/{id}/hod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> removeHod(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.removeHod(id)
        );
    }
}
