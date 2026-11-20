package com.institute.ims.config;

import com.institute.ims.model.Department;
import com.institute.ims.model.FacultyProfileOption;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.FacultyProfileOptionRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final FacultyProfileOptionRepository facultyProfileOptionRepository =
            mock(FacultyProfileOptionRepository.class);
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final Map<String, User> usersByRollNumber = new HashMap<>();
    private final Map<String, Department> departmentsByCode = new HashMap<>();
    private final Map<String, Subject> subjectsByCode = new HashMap<>();
    private final AtomicLong userIds = new AtomicLong();
    private final AtomicLong departmentIds = new AtomicLong();
    private final AtomicLong subjectIds = new AtomicLong();

    @BeforeEach
    void setUp() {
        when(userRepository.findByRollNumber(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(usersByRollNumber.get(invocation.getArgument(0))));
        when(userRepository.findByIdWithDepartments(anyLong())).thenAnswer(invocation ->
                usersByRollNumber.values().stream()
                        .filter(user -> user.getId().equals(invocation.getArgument(0)))
                        .findFirst());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(userIds.incrementAndGet());
            }
            usersByRollNumber.put(user.getRollNumber(), user);
            return user;
        });

        when(departmentRepository.findByCode(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(departmentsByCode.get(invocation.getArgument(0))));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            if (department.getId() == null) {
                department.setId(departmentIds.incrementAndGet());
            }
            departmentsByCode.put(department.getCode(), department);
            return department;
        });

        when(subjectRepository.existsByCode(anyString())).thenAnswer(invocation ->
                subjectsByCode.containsKey(invocation.getArgument(0)));
        when(subjectRepository.findByCode(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(subjectsByCode.get(invocation.getArgument(0))));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject subject = invocation.getArgument(0);
            if (subject.getId() == null) {
                subject.setId(subjectIds.incrementAndGet());
            }
            subjectsByCode.put(subject.getCode(), subject);
            return subject;
        });
        when(subjectRepository.findByDepartmentIdAndSemester(anyLong(), anyInt())).thenAnswer(invocation ->
                subjectsForDepartment(invocation.getArgument(0)).stream()
                        .filter(subject -> subject.getSemester().equals(invocation.getArgument(1)))
                        .toList());
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(anyLong())).thenAnswer(invocation ->
                subjectsForDepartment(invocation.getArgument(0)));
        when(facultyProfileOptionRepository.existsByCategoryAndValueIgnoreCase(
                any(FacultyProfileOption.Category.class), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
                "encoded-" + invocation.getArgument(0));
    }

    @Test
    void initializesEveryDepartmentWithAnHodWhoTeachesAndSeedsCoreAccountsIdempotently() throws Exception {
        CommandLineRunner initializer = new DataInitializer().initializeData(
                userRepository,
                departmentRepository,
                facultyProfileOptionRepository,
                subjectRepository,
                passwordEncoder,
                jdbcTemplate);

        initializer.run();
        initializer.run();

        assertThat(departmentsByCode).hasSize(5);
        assertThat(departmentsByCode.values()).allSatisfy(department -> {
            User hod = department.getHod();
            assertThat(hod).isNotNull();
            assertThat(hod.getRole()).isEqualTo(Role.HOD);
            assertThat(hod.getAdministrativeRole()).isEqualTo("HOD");
            assertThat(subjectsForDepartment(department.getId()))
                    .anySatisfy(subject -> assertThat(subject.getFacultyMembers())
                            .anyMatch(faculty -> faculty.getId().equals(hod.getId())));
        });
        assertThat(usersByRollNumber.values()).filteredOn(user -> user.getRole() == Role.HOD).hasSize(5);
        assertThat(usersByRollNumber.values()).filteredOn(user -> user.getRole() == Role.ADMIN).hasSize(1);
        assertThat(usersByRollNumber.values()).filteredOn(user -> user.getRole() == Role.FACULTY).hasSize(1);
        assertThat(usersByRollNumber.values()).filteredOn(user -> user.getRole() == Role.STUDENT).hasSize(1);
    }

    private List<Subject> subjectsForDepartment(Long departmentId) {
        return subjectsByCode.values().stream()
                .filter(subject -> subject.getDepartment().getId().equals(departmentId))
                .toList();
    }
}
