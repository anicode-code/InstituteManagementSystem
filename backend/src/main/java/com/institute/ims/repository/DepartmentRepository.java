package com.institute.ims.repository;

import com.institute.ims.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    boolean existsByCode(String code);
}
