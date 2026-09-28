package com.institute.ims.controller;

import com.institute.ims.model.Department;
import com.institute.ims.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Department> create(
            @RequestBody Department department) {

        return ResponseEntity.ok(
                service.create(department)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> update(
            @PathVariable Long id,
            @RequestBody Department department) {

        return ResponseEntity.ok(
                service.update(id, department)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Department deleted successfully"
        );
    }
}
