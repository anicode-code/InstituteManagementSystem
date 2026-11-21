package com.institute.ims.service;

import com.institute.ims.dto.UpdateProfileRequest;
import com.institute.ims.model.Department;
import com.institute.ims.model.FacultyProfileAudit;
import com.institute.ims.model.FacultyProfileOption;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.SubjectType;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.FacultyProfileAuditRepository;
import com.institute.ims.repository.FacultyProfileOptionRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private FacultyProfileOptionRepository facultyProfileOptionRepository;

    @Mock
    private FacultyProfileAuditRepository facultyProfileAuditRepository;

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
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("student@example.edu", 1L)).thenReturn(false);
        when(subjectRepository.findByDepartmentIdAndSemester(10L, 3))
                .thenReturn(List.of(compulsory, elective));
        when(userRepository.save(student)).thenReturn(student);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Updated Student");
        request.setEmail("student@example.edu");
        request.setYear(2);
        request.setSemester(3);
        request.setOptionalSubjectCodes(List.of("CS305"));
        request.setRegistrationNumber("REG-1001");
        request.setPhone("9876543210");
        request.setDateOfBirth(LocalDate.of(2002, 4, 12));
        request.setResidentialAddress("12 Campus Road");
        request.setSemesterSgpas(Map.of(1, 8.0, 2, 9.0));

        when(userRepository.existsByRegistrationNumberIgnoreCaseAndIdNot("REG-1001", 1L)).thenReturn(false);
        User updated = service.updateOwnProfile("STUDENT", request);

        assertThat(updated.getYear()).isEqualTo(2);
        assertThat(updated.getSemester()).isEqualTo(3);
        assertThat(updated.getSelectedSubjects()).containsExactlyInAnyOrder(compulsory, elective);
        assertThat(updated.getName()).isEqualTo("Updated Student");
        assertThat(updated.getRegistrationNumber()).isEqualTo("REG-1001");
        assertThat(updated.getPhone()).isEqualTo("9876543210");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(2002, 4, 12));
        assertThat(updated.getResidentialAddress()).isEqualTo("12 Campus Road");
        assertThat(updated.getSemesterSgpas()).containsExactlyInAnyOrderEntriesOf(Map.of(1, 8.0, 2, 9.0));
        assertThat(updated.getYearlyGpas()).containsEntry(1, 8.5);
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
    void studentCannotEnterAnInvalidOrCurrentSemesterSgpa() {
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student()));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Student");
        request.setSemesterSgpas(Map.of(3, 8.5));

        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("previous semester");

        request.setSemesterSgpas(Map.of(1, 10.1));
        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("0 to 10");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void studentCannotSetAFutureDateOfBirthOrDuplicateRegistrationNumber() {
        User student = student();
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student));

        UpdateProfileRequest invalidPhone = new UpdateProfileRequest();
        invalidPhone.setName("Student");
        invalidPhone.setPhone("98ABC54321");
        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", invalidPhone))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("10-digit Indian mobile number");

        UpdateProfileRequest futureBirthDate = new UpdateProfileRequest();
        futureBirthDate.setName("Student");
        futureBirthDate.setDateOfBirth(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", futureBirthDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be in the future");

        UpdateProfileRequest duplicateRegistration = new UpdateProfileRequest();
        duplicateRegistration.setName("Student");
        duplicateRegistration.setRegistrationNumber("REG-1001");
        when(userRepository.existsByRegistrationNumberIgnoreCaseAndIdNot("REG-1001", 1L))
                .thenReturn(true);
        assertThatThrownBy(() -> service.updateOwnProfile("STUDENT", duplicateRegistration))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Registration number already exists");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void facultyCanUpdatePersonalAndProfessionalProfileDetails() {
        User faculty = new User();
        faculty.setId(2L);
        faculty.setName("Faculty");
        faculty.setRollNumber("FACULTY");
        faculty.setRole(Role.FACULTY);
        faculty.setAdministrativeRole("NONE");
        faculty.setDepartment(department());
        faculty.setStatus(com.institute.ims.model.VerificationStatus.VERIFIED);
        when(userRepository.findByRollNumber("FACULTY")).thenReturn(Optional.of(faculty));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("faculty@example.edu", 2L)).thenReturn(false);
        when(userRepository.save(faculty)).thenReturn(faculty);
        when(facultyProfileOptionRepository.existsByCategoryAndValue(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Faculty");
        request.setEmail("faculty@example.edu");
        request.setPhone("9876543210");
        request.setDateOfBirth(LocalDate.of(1980, 4, 12));
        request.setGender("Other");
        request.setResidentialAddress("12 Campus Road");
        request.setJoiningDate(LocalDate.of(2010, 8, 1));
        request.setEmploymentStatus("Employed");
        request.setDesignation("PROFESSOR");
        request.setQualification("PhD");
        request.setSpecialization("Computer Science");
        request.setYearsOfExperience(15.5);
        request.setEmploymentType("Permanent");

        User updated = service.updateOwnProfile("FACULTY", request);

        assertThat(updated.getPhone()).isEqualTo("9876543210");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(1980, 4, 12));
        assertThat(updated.getGender()).isEqualTo("Other");
        assertThat(updated.getResidentialAddress()).isEqualTo("12 Campus Road");
        assertThat(updated.getJoiningDate()).isEqualTo(LocalDate.of(2010, 8, 1));
        assertThat(updated.getEmploymentStatus()).isEqualTo("Employed");
        assertThat(updated.getDesignation()).isEqualTo("PROFESSOR");
        assertThat(updated.getAdministrativeRole()).isEqualTo("NONE");
        assertThat(updated.getQualification()).isEqualTo("PhD");
        assertThat(updated.getSpecialization()).isEqualTo("Computer Science");
        assertThat(updated.getYearsOfExperience()).isEqualTo(15.5);
        assertThat(updated.getEmploymentType()).isEqualTo("Permanent");
        assertThat(updated.getDepartments()).isEmpty();
        assertThat(updated.getRollNumber()).isEqualTo("FACULTY");
        assertThat(updated.getDepartment()).isSameAs(faculty.getDepartment());
        assertThat(updated.getStatus()).isEqualTo(com.institute.ims.model.VerificationStatus.VERIFIED);

        UpdateProfileRequest clearOptionalValues = validFacultyRequest();
        clearOptionalValues.setDateOfBirth(null);
        clearOptionalValues.setYearsOfExperience(null);

        User cleared = service.updateOwnProfile("FACULTY", clearOptionalValues);

        assertThat(cleared.getDateOfBirth()).isNull();
        assertThat(cleared.getYearsOfExperience()).isNull();
        assertThat(cleared.getPhone()).isEqualTo("9876543210");
        verify(facultyProfileAuditRepository, org.mockito.Mockito.atLeastOnce()).saveAll(
                org.mockito.ArgumentMatchers.<Iterable<FacultyProfileAudit>>argThat(audits ->
                        {
                            for (FacultyProfileAudit audit : audits) {
                                if (audit.getFieldName().equals("phone")
                                        && audit.getPreviousValue() == null
                                        && audit.getUpdatedValue().equals("9876543210")
                                        && audit.getChangedAt() != null) {
                                    return true;
                                }
                            }
                            return false;
                        }));
    }

    @Test
    void hodCanUpdateTheSamePersonalAndProfessionalProfileFieldsAsFaculty() {
        User hod = new User();
        hod.setId(3L);
        hod.setName("HOD");
        hod.setRollNumber("HOD");
        hod.setRole(Role.HOD);
        hod.setAdministrativeRole("HOD");
        hod.setDepartment(department());
        when(userRepository.findByRollNumber("HOD")).thenReturn(Optional.of(hod));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("hod@example.edu", 3L)).thenReturn(false);
        when(userRepository.save(hod)).thenReturn(hod);
        when(facultyProfileOptionRepository.existsByCategoryAndValue(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Updated HOD");
        request.setEmail("hod@example.edu");
        request.setPhone("9876543210");
        request.setDateOfBirth(LocalDate.of(1978, 5, 10));
        request.setGender("Female");
        request.setResidentialAddress("24 Faculty Road");
        request.setJoiningDate(LocalDate.of(2008, 7, 1));
        request.setEmploymentStatus("Employed");
        request.setDesignation("ASSOCIATE PROFESSOR");
        request.setQualification("PhD");
        request.setSpecialization("Information Systems");
        request.setYearsOfExperience(18.0);
        request.setEmploymentType("Permanent");

        User updated = service.updateOwnProfile("HOD", request);

        assertThat(updated.getName()).isEqualTo("Updated HOD");
        assertThat(updated.getPhone()).isEqualTo("9876543210");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(1978, 5, 10));
        assertThat(updated.getGender()).isEqualTo("Female");
        assertThat(updated.getResidentialAddress()).isEqualTo("24 Faculty Road");
        assertThat(updated.getJoiningDate()).isEqualTo(LocalDate.of(2008, 7, 1));
        assertThat(updated.getEmploymentStatus()).isEqualTo("Employed");
        assertThat(updated.getDesignation()).isEqualTo("ASSOCIATE PROFESSOR");
        assertThat(updated.getQualification()).isEqualTo("PhD");
        assertThat(updated.getSpecialization()).isEqualTo("Information Systems");
        assertThat(updated.getYearsOfExperience()).isEqualTo(18.0);
        assertThat(updated.getEmploymentType()).isEqualTo("Permanent");
        assertThat(updated.getAdministrativeRole()).isEqualTo("HOD");
    }

    @Test
    void facultyCannotChangeDepartmentsThroughOwnProfile() {
        User faculty = new User();
        faculty.setId(2L);
        faculty.setRollNumber("FACULTY");
        faculty.setRole(Role.FACULTY);
        when(userRepository.findByRollNumber("FACULTY")).thenReturn(Optional.of(faculty));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDepartmentIds(List.of(10L));

        assertThatThrownBy(() -> service.updateOwnProfile("FACULTY", request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("assigned through subject teaching approvals");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void facultyCannotChangeAdministrativeRoleThroughOwnProfile() {
        User faculty = new User();
        faculty.setId(2L);
        faculty.setRollNumber("FACULTY");
        faculty.setRole(Role.FACULTY);
        faculty.setAdministrativeRole("NONE");
        when(userRepository.findByRollNumber("FACULTY")).thenReturn(Optional.of(faculty));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Faculty");
        request.setAdministrativeRole("HOD");

        assertThatThrownBy(() -> service.updateOwnProfile("FACULTY", request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("only be managed by an administrator");

        assertThat(faculty.getAdministrativeRole()).isEqualTo("NONE");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void facultyCannotSetAnOptionThatIsNotPresentInTheDatabase() {
        User faculty = new User();
        faculty.setId(2L);
        faculty.setRollNumber("FACULTY");
        faculty.setRole(Role.FACULTY);
        when(userRepository.findByRollNumber("FACULTY")).thenReturn(Optional.of(faculty));
        when(facultyProfileOptionRepository.existsByCategoryAndValue(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        when(facultyProfileOptionRepository.existsByCategoryAndValue(
                FacultyProfileOption.Category.GENDER, "Invalid")).thenReturn(false);

        UpdateProfileRequest request = validFacultyRequest();
        request.setGender("Invalid");

        assertThatThrownBy(() -> service.updateOwnProfile("FACULTY", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("valid gender");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void facultyJoiningDateCannotPrecedeDateOfBirth() {
        User faculty = new User();
        faculty.setId(2L);
        faculty.setRollNumber("FACULTY");
        faculty.setRole(Role.FACULTY);
        when(userRepository.findByRollNumber("FACULTY")).thenReturn(Optional.of(faculty));
        when(facultyProfileOptionRepository.existsByCategoryAndValue(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        UpdateProfileRequest request = validFacultyRequest();
        request.setDateOfBirth(LocalDate.of(2010, 1, 1));
        request.setJoiningDate(LocalDate.of(2009, 1, 1));

        assertThatThrownBy(() -> service.updateOwnProfile("FACULTY", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be before date of birth");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void everyRoleCanUploadAValidProfilePicture() throws IOException {
        byte[] image = validPng();

        for (Role role : Role.values()) {
            User user = new User();
            user.setId((long) role.ordinal() + 1);
            user.setName(role.name());
            user.setRollNumber(role.name());
            user.setRole(role);
            when(userRepository.findByRollNumber(role.name())).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            User updated = service.updateProfilePicture(
                    role.name(),
                    new MockMultipartFile("file", "portrait.png", "image/png", image));

            assertThat(updated.getProfilePicture()).isNotEmpty().isNotEqualTo(image);
            assertThat(updated.getProfilePictureContentType()).isEqualTo("image/jpeg");
            assertThat(updated.getProfileImageUrl()).contains("/api/users/" + user.getId() + "/profile-picture?v=");
            assertThat(ImageIO.read(new ByteArrayInputStream(updated.getProfilePicture()))).isNotNull();
        }
    }

    @Test
    void profilePictureUploadRejectsInvalidImagesAndOversizedFiles() {
        MockMultipartFile invalidImage =
                new MockMultipartFile("file", "portrait.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> service.updateProfilePicture("STUDENT", invalidImage))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("not a valid image");

        MockMultipartFile oversizedImage = new MockMultipartFile(
                "file",
                "portrait.png",
                "image/png",
                new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.updateProfilePicture("STUDENT", oversizedImage))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("5 MB or smaller");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void nonFacultyCannotUpdateFacultyProfileDetails() {
        when(userRepository.findByRollNumber("STUDENT")).thenReturn(Optional.of(student()));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Student");
        request.setGender("Other");

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
        student.setYear(2);
        student.setSemester(3);
        return student;
    }

    private Department department() {
        Department department = new Department();
        department.setId(10L);
        department.setCode("CSE");
        return department;
    }

    private UpdateProfileRequest validFacultyRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Faculty");
        request.setPhone("9876543210");
        request.setGender("Other");
        request.setJoiningDate(LocalDate.of(2010, 8, 1));
        request.setEmploymentStatus("Employed");
        request.setDesignation("PROFESSOR");
        request.setQualification("PhD");
        request.setEmploymentType("Permanent");
        return request;
    }

    private Subject subject(String code, SubjectType type) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setType(type);
        return subject;
    }

    private byte[] validPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
