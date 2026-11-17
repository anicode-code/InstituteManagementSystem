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

import java.time.LocalDate;
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

    @Test
    void facultyCanUpdatePersonalAndProfessionalProfileDetails() {
        User faculty = new User();
        faculty.setId(2L);
        faculty.setName("Faculty");
        faculty.setRollNumber("FACULTY");
        faculty.setRole(Role.FACULTY);
        faculty.setDepartment(department());
        faculty.setStatus(com.institute.ims.model.VerificationStatus.VERIFIED);
        when(userRepository.findByRollNumber("FACULTY")).thenReturn(Optional.of(faculty));
        when(userRepository.existsByEmailAndIdNot("faculty@example.edu", 2L)).thenReturn(false);
        when(userRepository.save(faculty)).thenReturn(faculty);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Faculty");
        request.setEmail("faculty@example.edu");
        request.setPhone("555-0100");
        request.setDateOfBirth(LocalDate.of(1980, 4, 12));
        request.setGender("Other");
        request.setResidentialAddress("12 Campus Road");
        request.setJoiningDate(LocalDate.of(2010, 8, 1));
        request.setEmploymentStatus("Active");
        request.setDesignation("Professor");
        request.setQualification("PhD");
        request.setSpecialization("Computer Science");
        request.setYearsOfExperience(15.5);
        request.setEmploymentType("Full-time");

        User updated = service.updateOwnProfile("FACULTY", request);

        assertThat(updated.getPhone()).isEqualTo("555-0100");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(1980, 4, 12));
        assertThat(updated.getGender()).isEqualTo("Other");
        assertThat(updated.getResidentialAddress()).isEqualTo("12 Campus Road");
        assertThat(updated.getJoiningDate()).isEqualTo(LocalDate.of(2010, 8, 1));
        assertThat(updated.getEmploymentStatus()).isEqualTo("Active");
        assertThat(updated.getDesignation()).isEqualTo("Professor");
        assertThat(updated.getQualification()).isEqualTo("PhD");
        assertThat(updated.getSpecialization()).isEqualTo("Computer Science");
        assertThat(updated.getYearsOfExperience()).isEqualTo(15.5);
        assertThat(updated.getEmploymentType()).isEqualTo("Full-time");
        assertThat(updated.getRollNumber()).isEqualTo("FACULTY");
        assertThat(updated.getDepartment()).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(com.institute.ims.model.VerificationStatus.VERIFIED);

        UpdateProfileRequest clearOptionalValues = new UpdateProfileRequest();
        clearOptionalValues.setName("Faculty");
        clearOptionalValues.setDateOfBirth(null);
        clearOptionalValues.setYearsOfExperience(null);

        User cleared = service.updateOwnProfile("FACULTY", clearOptionalValues);

        assertThat(cleared.getDateOfBirth()).isNull();
        assertThat(cleared.getYearsOfExperience()).isNull();
        assertThat(cleared.getPhone()).isEqualTo("555-0100");
    }

    @Test
    void nonFacultyCannotUpdateFacultyProfileDetails() {
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student()));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Student");
        request.setPhone("555-0100");

        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Only faculty");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
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
