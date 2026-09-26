package com.institute.ims.department;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
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
}