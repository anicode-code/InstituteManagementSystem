package com.institute.ims.repository;

import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
