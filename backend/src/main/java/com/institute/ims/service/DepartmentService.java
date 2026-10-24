package com.institute.ims.service;

import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository repository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository repository,
                             UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<Department> getAll() {
        return repository.findAll();
    }

    public Department getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Department not found"));
    }

    public Department create(Department department) {

        if (repository.existsByCode(department.getCode())) {
            throw new RuntimeException(
                    "Department code already exists"
            );
        }

        return repository.save(department);
    }

    public Department update(
            Long id,
            Department request) {

        Department department = getById(id);

        department.setName(request.getName());
        department.setCode(request.getCode());

        return repository.save(department);
    }

    public void delete(Long id) {

        if (!repository.existsById(id)) {
            throw new RuntimeException(
                    "Department not found"
            );
        }

        repository.deleteById(id);
    }

    // ─── HOD Management ───────────────────────────────────────────────

    /**
     * Get eligible faculty for HOD assignment.
     * Returns verified faculty/HOD users who registered with this department.
     */
    public List<User> getEligibleHods(Long departmentId) {
        // Verify department exists
        getById(departmentId);

        return userRepository.findByDepartmentsContainingAndRoleInAndStatus(
                departmentId,
                List.of(Role.FACULTY, Role.HOD),
                VerificationStatus.VERIFIED
        );
    }

    /**
     * Assign or replace an HOD for a department.
     * Rules:
     *   - The faculty must be verified and have this department in their departments set.
     *   - If the department already has an HOD, the old HOD is removed first.
     *   - If the old HOD has no remaining HOD positions, their role reverts to FACULTY.
     *   - The new HOD's role is set to HOD.
     */
    public Department assignHod(Long departmentId, Long facultyId) {
        Department department = getById(departmentId);

        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // Verify the faculty is eligible
        if (faculty.getStatus() != VerificationStatus.VERIFIED) {
            throw new RuntimeException("Only verified faculty can be promoted to HOD.");
        }

        if (faculty.getRole() != Role.FACULTY && faculty.getRole() != Role.HOD) {
            throw new RuntimeException("Only faculty members can be promoted to HOD.");
        }

        // Check that the faculty has this department in their departments set or primary department
        boolean belongsToDept = (faculty.getDepartment() != null && faculty.getDepartment().getId().equals(departmentId))
                || (faculty.getDepartments() != null && faculty.getDepartments().stream().anyMatch(d -> d.getId().equals(departmentId)));
        if (!belongsToDept) {
            throw new RuntimeException(
                    "This faculty member did not register with this department and cannot be assigned as its HOD.");
        }

        // Remove old HOD if exists
        if (department.getHod() != null) {
            User oldHod = department.getHod();
            department.setHod(null);
            repository.save(department);

            // Check if old HOD is still HOD of any other department
            List<Department> otherHodDepts = repository.findByHodId(oldHod.getId());
            if (otherHodDepts.isEmpty()) {
                // Revert old HOD's role to FACULTY
                oldHod.setRole(Role.FACULTY);
                userRepository.save(oldHod);
            }
        }

        // Assign new HOD
        department.setHod(faculty);
        faculty.setRole(Role.HOD);
        userRepository.save(faculty);

        return repository.save(department);
    }

    /**
     * Remove the HOD from a department.
     * If the removed HOD has no remaining HOD positions, their role reverts to FACULTY.
     */
    public Department removeHod(Long departmentId) {
        Department department = getById(departmentId);

        if (department.getHod() == null) {
            throw new RuntimeException("This department has no HOD assigned.");
        }

        User oldHod = department.getHod();
        department.setHod(null);
        repository.save(department);

        // Check if old HOD is still HOD of any other department
        List<Department> otherHodDepts = repository.findByHodId(oldHod.getId());
        if (otherHodDepts.isEmpty()) {
            oldHod.setRole(Role.FACULTY);
            userRepository.save(oldHod);
        }

        return department;
    }
}
