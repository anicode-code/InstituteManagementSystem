package com.institute.ims.repository;

import com.institute.ims.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    boolean existsByCode(String code);

    Optional<Department> findByCode(String code);

    List<Department> findByHodId(Long hodId);
}

