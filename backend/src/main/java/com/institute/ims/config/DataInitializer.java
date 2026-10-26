package com.institute.ims.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.institute.ims.model.*;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initializeData(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            SubjectRepository subjectRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {


        return args -> {

            // ─── 1. Seed Departments ───────────────────────────────────────
            Department cse   = createDeptIfNotExists(departmentRepository, "Computer Science", "CSE");
            Department it    = createDeptIfNotExists(departmentRepository, "Information Technology", "IT");
            Department ece   = createDeptIfNotExists(departmentRepository, "Electronics & Communication", "ECE");
            Department me    = createDeptIfNotExists(departmentRepository, "Mechanical Engineering", "ME");
            Department civil = createDeptIfNotExists(departmentRepository, "Civil Engineering", "CIVIL");

            // ─── 2. Seed Subjects from curriculum_subjects.json ────────────
            seedSubjectsFromJson(departmentRepository, subjectRepository);

            // ─── 3. Seed Admin user ────────────────────────────────────────
            createUserIfNotExists(
                    userRepository, passwordEncoder,
                    "System Admin", "ADMIN001", "admin123",
                    "admin@ims.com", Role.ADMIN, cse,
                    VerificationStatus.VERIFIED, null
            );

            // ─── 4. Seed HOD ───────────────────────────────────────────────
            // createUserIfNotExists will add `cse` to the user's departments set
            User cseFaculty = createUserIfNotExists(
                    userRepository, passwordEncoder,
                    "CSE HOD", "HOD001", "cse_hod123",
                    "cse_hod@ims.com", Role.HOD, cse,
                    VerificationStatus.VERIFIED, cse
            );

            // Link this HOD as the HOD of CSE department (only if not already set)
            Department freshCse = departmentRepository.findByCode("CSE").orElse(cse);
            if (cseFaculty != null && freshCse.getHod() == null) {
                freshCse.setHod(cseFaculty);
                departmentRepository.save(freshCse);
            }

            // ─── 5. Seed Faculty ───────────────────────────────────────────
            // createUserIfNotExists will add `cse` to the user's departments set
            createUserIfNotExists(
                    userRepository, passwordEncoder,
                    "Faculty 1", "FAC001", "faculty123",
                    "faculty@ims.com", Role.FACULTY, cse,
                    VerificationStatus.VERIFIED, cse
            );

            // ─── 6. Seed Demo Student ──────────────────────────────────────
            User demoStudent = createUserIfNotExists(
                    userRepository, passwordEncoder,
                    "Demo Student", "2026CS001", "demo_student123",
                    "demo_student@ims.com", Role.STUDENT, cse,
                    VerificationStatus.VERIFIED, null
            );

            if (demoStudent != null && demoStudent.getSemester() == null) {
                demoStudent.setYear(1);
                demoStudent.setSemester(1);
                Set<Subject> demoSubjects = new HashSet<>();
                demoSubjects.addAll(subjectRepository.findByDepartmentIdAndSemester(cse.getId(), 1)
                        .stream().filter(s -> s.getType() == SubjectType.COMPULSORY).toList());
                subjectRepository.findByCode("CS105").ifPresent(demoSubjects::add);
                demoStudent.setSelectedSubjects(demoSubjects);
                userRepository.save(demoStudent);
            }

            jdbcTemplate.update("""
                    INSERT INTO subject_faculty (subject_id, faculty_id)
                    SELECT id, faculty_id FROM subjects WHERE faculty_id IS NOT NULL
                    ON CONFLICT DO NOTHING
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS ux_faculty_subject_requests_pending
                    ON faculty_subject_requests (faculty_id, subject_id)
                    WHERE status = 'PENDING'
                    """);
        };
    }

    private void seedSubjectsFromJson(
            DepartmentRepository departmentRepository,
            SubjectRepository subjectRepository) {

        try {
            ClassPathResource resource = new ClassPathResource("data/curriculum_subjects.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inputStream);

            JsonNode departments = root.get("departments");
            if (departments == null) return;

            for (JsonNode deptNode : departments) {
                String deptCode = deptNode.get("code").asText();

                Department dept = departmentRepository.findByCode(deptCode)
                        .orElse(null);

                if (dept == null) continue;

                JsonNode semesters = deptNode.get("semesters");
                if (semesters == null) continue;

                semesters.fields().forEachRemaining(semEntry -> {
                    int semNumber = Integer.parseInt(semEntry.getKey());
                    JsonNode semNode = semEntry.getValue();
                    int yearNumber = semNode.has("year") ? semNode.get("year").asInt() : ((semNumber + 1) / 2);

                    // Process compulsory subjects
                    JsonNode compulsory = semNode.get("compulsory");
                    if (compulsory != null) {
                        for (JsonNode subNode : compulsory) {
                            createSubjectIfNotExists(subjectRepository, dept, subNode, semNumber, yearNumber, SubjectType.COMPULSORY);
                        }
                    }

                    // Process optional subjects
                    JsonNode optional = semNode.get("optional");
                    if (optional != null) {
                        for (JsonNode subNode : optional) {
                            createSubjectIfNotExists(subjectRepository, dept, subNode, semNumber, yearNumber, SubjectType.OPTIONAL);
                        }
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("[DataInitializer] Warning: Could not seed subjects from JSON: " + e.getMessage());
        }
    }

    private void createSubjectIfNotExists(
            SubjectRepository subjectRepository,
            Department dept,
            JsonNode subNode,
            int semester,
            int academicYear,
            SubjectType type) {

        String code = subNode.get("code").asText();

        if (subjectRepository.existsByCode(code)) {
            return;
        }

        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(subNode.get("name").asText());
        subject.setCredits(subNode.has("credits") ? subNode.get("credits").asInt() : null);
        subject.setSemester(semester);
        subject.setAcademicYear(academicYear);
        subject.setType(type);
        subject.setDepartment(dept);
        // faculty is null for seeded curriculum subjects (assigned later by admin)

        subjectRepository.save(subject);
    }

    private Department createDeptIfNotExists(
            DepartmentRepository repository,
            String name,
            String code) {

        return repository.findByCode(code)
                .orElseGet(() -> repository.save(new Department(name, code)));
    }

    /**
     * Creates a user if they don't already exist.
     * @param facultyDept if non-null, this department will be added to the user's departments set (for faculty multi-dept).
     * @return the newly-created user or null if it already existed.
     */
    private User createUserIfNotExists(
            UserRepository repository,
            PasswordEncoder encoder,
            String name,
            String rollNumber,
            String password,
            String email,
            Role role,
            Department department,
            VerificationStatus status,
            Department facultyDept) {

        if (repository.existsByRollNumber(rollNumber)) {
            return repository.findByRollNumber(rollNumber).orElse(null);
        }

        User user = new User();

        user.setName(name);
        user.setRollNumber(rollNumber);
        user.setPassword(encoder.encode(password));
        user.setEmail(email);
        user.setRole(role);
        user.setDepartment(department);
        user.setStatus(status);

        if (facultyDept != null) {
            user.getDepartments().add(facultyDept);
        }

        return repository.save(user);
    }
}