package com.institute.ims.dto;

import com.institute.ims.model.Department;

public record DepartmentSummaryResponse(Long id, String name, String code) {
    public static DepartmentSummaryResponse from(Department department) {
        return new DepartmentSummaryResponse(
                department.getId(), department.getName(), department.getCode());
    }
}
