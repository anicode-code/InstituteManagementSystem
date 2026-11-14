package com.institute.ims.service;

import com.institute.ims.dto.UpdateProfileRequest;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    void studentCanUpdateAcademicDetailsAndSelectOnlyCurrentSemesterElectives() {
        User student = student();
        Subject compulsory = subject("CS301", SubjectType.COMPULSORY);
        Subject elective = subject("CS305", SubjectType.OPTIONAL);
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student));
        when(userRepository.existsByEmailAndIdNot("student@example.edu", 1L)).thenReturn(false);
        when(subjectRepository.findByDepartmentIdAndSemester(10L, 3))
                .thenReturn(List.of(compulsory, elective));
        when(userRepository.save(student)).thenReturn(student);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Updated Student");
        request.setEmail("student@example.edu");
        request.setYear(2);
        request.setSemester(3);
        request.setOptionalSubjectCodes(List.of("CS305"));

        User updated = service.updateOwnProfile("STUDENT", request);

        assertThat(updated.getYear()).isEqualTo(2);
        assertThat(updated.getSemester()).isEqualTo(3);
        assertThat(updated.getSelectedSubjects()).containsExactlyInAnyOrder(compulsory, elective);
        assertThat(updated.getName()).isEqualTo("Updated Student");
    }

    @Test
    void studentCannotSelectAnElectiveOutsideTheirDepartmentAndSemester() {
        User student = student();
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student));
        when(subjectRepository.findByDepartmentIdAndSemester(10L, 1))
                .thenReturn(List.of(subject("CS101", SubjectType.COMPULSORY)));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Student");
        request.setYear(1);
        request.setSemester(1);
        request.setOptionalSubjectCodes(List.of("CS305"));

        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("optional subjects");

        verify(userRepository, never()).save(student);
    }

    @Test
    void studentAcademicYearMustMatchSemester() {
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student()));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Student");
        request.setYear(1);
        request.setSemester(3);

        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("matching semester");

        verify(subjectRepository, never()).findByDepartmentIdAndSemester(10L, 3);
    }

    private User student() {
        User student = new User();
        student.setId(1L);
        student.setName("Student");
        student.setRollNumber("STUDENT");
        student.setRole(Role.STUDENT);
        student.setDepartment(department());
        return student;
    }

    private Department department() {
        Department department = new Department();
        department.setId(10L);
        department.setCode("CSE");
        return department;
    }

    private Subject subject(String code, SubjectType type) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setType(type);
        return subject;
    }
}
