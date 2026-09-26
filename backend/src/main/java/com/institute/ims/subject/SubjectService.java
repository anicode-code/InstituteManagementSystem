package com.institute.ims.subject;

import com.institute.ims.department.Department;
import com.institute.ims.department.DepartmentRepository;
import com.institute.ims.user.User;
import com.institute.ims.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public SubjectService(
            SubjectRepository subjectRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {

        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    public List<Subject> getAll() {
        return subjectRepository.findAll();
    }

    public Subject getById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));
    }

    public List<Subject> getByFaculty(Long facultyId) {
        return subjectRepository.findByFacultyId(facultyId);
    }

    public Subject create(
            String name,
            String code,
            Integer credits,
            Long departmentId,
            Long facultyId) {

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"));

        User faculty =
                userRepository.findById(facultyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Faculty not found"));

        Subject subject = new Subject();

        subject.setName(name);
        subject.setCode(code);
        subject.setCredits(credits);
        subject.setDepartment(department);
        subject.setFaculty(faculty);

        return subjectRepository.save(subject);
    }

    public Subject update(
            Long id,
            String name,
            String code,
            Integer credits) {

        Subject subject = getById(id);

        subject.setName(name);
        subject.setCode(code);
        subject.setCredits(credits);

        return subjectRepository.save(subject);
    }

    public void delete(Long id) {
        subjectRepository.deleteById(id);
    }
}