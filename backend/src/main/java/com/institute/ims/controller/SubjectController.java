package com.institute.ims.controller;

import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.service.SubjectService;
import com.institute.ims.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService service;
    private final UserService userService;
    private final DepartmentRepository departmentRepository;

    public SubjectController(
            SubjectService service,
            UserService userService,
            DepartmentRepository departmentRepository) {
        this.service = service;
        this.userService = userService;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Subject>> getAll(Authentication authentication) {
        if (authentication != null) {
            try {
                User currentUser = userService.getUserByRollNumber(authentication.getName());
                if (currentUser != null) {
                    if (currentUser.getRole() == Role.STUDENT) {
                        return ResponseEntity.ok(service.getByStudent(currentUser.getId()));
                    } else if (currentUser.getRole() == Role.HOD) {
                        Department dept = currentUser.getDepartment();
                        if (dept == null) {
                            List<Department> hodDepts = departmentRepository.findByHodId(currentUser.getId());
                            if (!hodDepts.isEmpty()) {
                                dept = hodDepts.get(0);
                            }
                        }
                        if (dept != null) {
                            return ResponseEntity.ok(service.getByDepartment(dept.getId()));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<Subject>> getByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(service.getByDepartment(deptId));
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

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Subject>> getByStudent(
            @PathVariable Long studentId,
            Authentication authentication) {

        if (authentication != null) {
            try {
                User currentUser = userService.getUserByRollNumber(authentication.getName());
                if (currentUser != null && currentUser.getRole() == Role.STUDENT && !currentUser.getId().equals(studentId)) {
                    throw new RuntimeException("Unauthorized: Students may only view their own subjects.");
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Unauthorized")) {
                    throw e;
                }
            }
        }

        return ResponseEntity.ok(
                service.getByStudent(studentId)
        );
    }

    /**
     * Get subjects for a specific department and semester.
     * Used by the registration page to display curriculum.
     * Query params: departmentId (required), semester (required)
     */
    @GetMapping("/curriculum")
    public ResponseEntity<List<Subject>> getCurriculum(
            @RequestParam Long departmentId,
            @RequestParam Integer semester) {

        return ResponseEntity.ok(
                service.getByDepartmentAndSemester(departmentId, semester)
        );
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public ResponseEntity<Subject> create(
            @RequestBody Map<String, Object> body) {

        String name = (String) body.get("name");
        String code = (String) body.get("code");

        Integer credits = body.get("credits") == null
                ? null
                : ((Number) body.get("credits")).intValue();

        Long departmentId =
                ((Number) body.get("departmentId")).longValue();

        Long facultyId = body.get("facultyId") == null
                ? null
                : ((Number) body.get("facultyId")).longValue();
        if (facultyId != null) {
            throw new AccessDeniedException("Use HOD subject assignment management to assign teaching faculty.");
        }

        Integer semester = body.get("semester") == null
                ? 1
                : ((Number) body.get("semester")).intValue();

        Integer academicYear = body.get("academicYear") == null
                ? (semester + 1) / 2
                : ((Number) body.get("academicYear")).intValue();

        SubjectType type = SubjectType.COMPULSORY;
        if (body.get("type") != null) {
            try {
                type = SubjectType.valueOf(body.get("type").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                service.create(
                        name,
                        code,
                        credits,
                        departmentId,
                        null,
                        semester,
                        academicYear,
                        type
                )
        );
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public ResponseEntity<Subject> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String name = (String) body.get("name");
        String code = (String) body.get("code");

        Integer credits = body.get("credits") == null
                ? null
                : ((Number) body.get("credits")).intValue();

        Long facultyId = body.get("facultyId") == null
                ? null
                : ((Number) body.get("facultyId")).longValue();
        if (facultyId != null) {
            throw new AccessDeniedException("Use HOD subject assignment management to change teaching faculty.");
        }

        Integer semester = body.get("semester") == null
                ? null
                : ((Number) body.get("semester")).intValue();

        Integer academicYear = body.get("academicYear") == null
                ? (semester != null ? (semester + 1) / 2 : null)
                : ((Number) body.get("academicYear")).intValue();

        SubjectType type = null;
        if (body.get("type") != null) {
            try {
                type = SubjectType.valueOf(body.get("type").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                service.update(
                        id,
                        name,
                        code,
                        credits,
                        null,
                        semester,
                        academicYear,
                        type
                )
        );
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Subject deleted successfully"
        );
    }
}
