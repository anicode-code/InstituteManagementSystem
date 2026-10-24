package com.institute.ims.repository;

import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByRollNumber(String rollNumber);

    boolean existsByRollNumber(String rollNumber);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartmentId(Long departmentId);

    List<User> findByStatus(VerificationStatus status);

    List<User> findByIdIn(List<Long> ids);

    List<User> findByRoleAndStatus(Role role, VerificationStatus status);

    // Find verified faculty/HOD users who have a specific department in their departments set or as primary department
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.departments d WHERE (d.id = :departmentId OR u.department.id = :departmentId) AND u.role IN :roles AND u.status = :status")
    List<User> findByDepartmentsContainingAndRoleInAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("roles") List<Role> roles,
            @Param("status") VerificationStatus status);
}

