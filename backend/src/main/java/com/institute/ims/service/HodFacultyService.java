package com.institute.ims.service;

import com.institute.ims.dto.HodFacultyRosterItem;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HodFacultyService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public HodFacultyService(UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<HodFacultyRosterItem> getRoster(String hodRollNumber) {
        User hod = userRepository.findByRollNumber(hodRollNumber)
                .orElseThrow(() -> new AccessDeniedException("HOD account not found."));
        if (hod.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only HODs may access the faculty roster.");
        }

        List<Department> departments = departmentRepository.findByHodId(hod.getId());
        if (departments.isEmpty()) {
            throw new AccessDeniedException("No department is assigned to this HOD account.");
        }

        Map<Long, User> facultyById = new LinkedHashMap<>();
        for (Department department : departments) {
            for (Role role : List.of(Role.FACULTY, Role.HOD)) {
                userRepository.findByRoleAndAssociatedDepartmentId(role, department.getId())
                        .forEach(faculty -> facultyById.putIfAbsent(faculty.getId(), faculty));
            }
            User departmentHod = department.getHod();
            if (departmentHod != null && departmentHod.getRole() == Role.HOD) {
                facultyById.putIfAbsent(departmentHod.getId(), departmentHod);
            }
        }

        return facultyById.values().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getRollNumber, String.CASE_INSENSITIVE_ORDER))
                .map(faculty -> toRosterItem(faculty, departments))
                .toList();
    }

    private HodFacultyRosterItem toRosterItem(User faculty, List<Department> hodDepartments) {
        Set<Long> facultyDepartmentIds = new HashSet<>();
        if (faculty.getDepartment() != null) {
            facultyDepartmentIds.add(faculty.getDepartment().getId());
        }
        if (faculty.getDepartments() != null) {
            faculty.getDepartments().stream()
                    .map(Department::getId)
                    .forEach(facultyDepartmentIds::add);
        }
        hodDepartments.stream()
                .filter(department -> department.getHod() != null
                        && Objects.equals(department.getHod().getId(), faculty.getId()))
                .map(Department::getId)
                .forEach(facultyDepartmentIds::add);

        List<String> departmentNames = hodDepartments.stream()
                .filter(department -> facultyDepartmentIds.contains(department.getId()))
                .map(Department::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new HodFacultyRosterItem(
                faculty.getId(),
                faculty.getRollNumber(),
                faculty.getRole(),
                faculty.getName(),
                faculty.getProfileImageUrl(),
                faculty.getEmail(),
                faculty.getPhone(),
                faculty.getDesignation(),
                faculty.getQualification(),
                faculty.getSpecialization(),
                faculty.getYearsOfExperience(),
                faculty.getEmploymentType(),
                faculty.getEmploymentStatus(),
                faculty.getStatus(),
                departmentNames);
    }
}
