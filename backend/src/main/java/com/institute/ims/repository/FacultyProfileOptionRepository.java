package com.institute.ims.repository;

import com.institute.ims.model.FacultyProfileOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyProfileOptionRepository extends JpaRepository<FacultyProfileOption, Long> {

    boolean existsByCategoryAndValueIgnoreCase(FacultyProfileOption.Category category, String value);

    boolean existsByCategoryAndValue(FacultyProfileOption.Category category, String value);
}
