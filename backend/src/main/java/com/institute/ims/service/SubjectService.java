package com.institute.ims.service;

import com.institute.ims.model.Department;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional(readOnly = true)
    public List<Subject> getByStudent(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Set<Subject> subjects = new LinkedHashSet<>();

        // Add subjects explicitly chosen / enrolled by the student
        if (student.getSelectedSubjects() != null) {
            subjects.addAll(student.getSelectedSubjects());
        }

        // Also ensure all compulsory subjects for student's department and semester are included
        if (student.getDepartment() != null && student.getSemester() != null) {
            List<Subject> compulsory = subjectRepository.findByDepartmentIdAndSemester(
                    student.getDepartment().getId(), student.getSemester()
            ).stream()
             .filter(s -> s.getType() == SubjectType.COMPULSORY)
             .toList();
            subjects.addAll(compulsory);
        }

        return new ArrayList<>(subjects);
    }

    public List<Subject> getByDepartment(Long departmentId) {
        return subjectRepository.findByDepartmentId(departmentId);
    }

    public List<Subject> getByDepartmentAndSemester(Long departmentId, Integer semester) {
        return subjectRepository.findByDepartmentIdAndSemester(departmentId, semester);
    }

    @Transactional
    public Subject create(
            String name,
            String code,
            Integer credits,
            Long departmentId,
            Long facultyId) {
        return create(name, code, credits, departmentId, facultyId, 1, 1, SubjectType.COMPULSORY);
    }

    @Transactional
    public Subject create(
            String name,
            String code,
            Integer credits,
            Long departmentId,
            Long facultyId,
            Integer semester,
            Integer academicYear,
            SubjectType type) {

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"));

        User faculty = null;
        if (facultyId != null) {
            faculty = userRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));
        }

        Subject subject = new Subject();

        subject.setName(name);
        subject.setCode(code);
        subject.setCredits(credits);
        subject.setDepartment(department);
        subject.setFaculty(faculty);
        subject.setSemester(semester != null ? semester : 1);
        subject.setAcademicYear(academicYear != null ? academicYear : (semester != null ? (semester + 1) / 2 : 1));
        subject.setType(type != null ? type : SubjectType.COMPULSORY);

        return subjectRepository.save(subject);
    }

    @Transactional
    public Subject update(
            Long id,
            String name,
            String code,
            Integer credits) {
        return update(id, name, code, credits, null, null, null, null);
    }

    @Transactional
    public Subject update(
            Long id,
            String name,
            String code,
            Integer credits,
            Long facultyId,
            Integer semester,
            Integer academicYear,
            SubjectType type) {

        Subject subject = getById(id);

        if (name != null && !name.isBlank()) subject.setName(name);
        if (code != null && !code.isBlank()) subject.setCode(code);
        if (credits != null) subject.setCredits(credits);
        if (semester != null) {
            subject.setSemester(semester);
            if (academicYear == null) {
                subject.setAcademicYear((semester + 1) / 2);
            }
        }
        if (academicYear != null) subject.setAcademicYear(academicYear);
        if (type != null) subject.setType(type);

        if (facultyId != null) {
            User faculty = userRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));
            subject.setFaculty(faculty);
        }

        return subjectRepository.save(subject);
    }

    public void delete(Long id) {
        subjectRepository.deleteById(id);
    }
}
