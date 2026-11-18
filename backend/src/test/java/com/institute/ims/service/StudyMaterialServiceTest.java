package com.institute.ims.service;

import com.institute.ims.dto.StudyMaterialResponse;
import com.institute.ims.model.Role;
import com.institute.ims.model.StudyMaterial;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.repository.StudyMaterialRepository;
import com.institute.ims.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyMaterialServiceTest {

    @Mock
    private StudyMaterialRepository studyMaterialRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private SubjectService subjectService;

    private StudyMaterialService service;

    @BeforeEach
    void setUp() {
        service = new StudyMaterialService(studyMaterialRepository, subjectRepository, subjectService);
    }

    @Test
    void facultyCanUploadFilesToAnAssignedSubject() {
        User faculty = user(10L, Role.FACULTY);
        Subject subject = subject(20L, faculty);
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));
        when(studyMaterialRepository.save(any(StudyMaterial.class)))
                .thenAnswer(invocation -> {
                    StudyMaterial material = invocation.getArgument(0);
                    material.setId(30L);
                    return material;
                });
        MockMultipartFile file = new MockMultipartFile(
                "files", "lecture.mp4", "video/mp4", new byte[]{1, 2, 3});

        List<StudyMaterialResponse> result = service.upload(
                20L, "Lecture", null, new MockMultipartFile[]{file}, faculty);

        assertThat(result).singleElement().satisfies(material -> {
            assertThat(material.fileName()).isEqualTo("lecture.mp4");
            assertThat(material.title()).isEqualTo("Lecture");
            assertThat(material.subjectId()).isEqualTo(20L);
            assertThat(material.uploadedByName()).isEqualTo("User 10");
            assertThat(material.uploadedAt()).isNotNull();
        });
        verify(studyMaterialRepository).save(any(StudyMaterial.class));
    }

    @Test
    void facultyCannotUploadToAnUnassignedSubject() {
        User faculty = user(10L, Role.FACULTY);
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject(20L, user(11L, Role.FACULTY))));

        assertThatThrownBy(() -> service.upload(
                20L,
                null,
                null,
                new MockMultipartFile[]{new MockMultipartFile("files", "notes.pdf", "application/pdf", new byte[]{1})},
                faculty))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(studyMaterialRepository);
    }

    @Test
    void studentMaterialsAreLimitedToTheirEnrolledSubjects() {
        User student = user(40L, Role.STUDENT);
        Subject enrolledSubject = subject(21L, user(10L, Role.FACULTY));
        when(subjectService.getByStudent(40L)).thenReturn(List.of(enrolledSubject));
        when(studyMaterialRepository.findMaterialResponsesBySubjectIds(List.of(21L)))
                .thenReturn(List.of());

        assertThat(service.getMaterials(student)).isEmpty();

        verify(studyMaterialRepository).findMaterialResponsesBySubjectIds(List.of(21L));
    }

    @Test
    void studentCannotDownloadMaterialForAnUnenrolledSubject() {
        User student = user(40L, Role.STUDENT);
        StudyMaterial material = new StudyMaterial();
        material.setSubject(subject(99L, user(10L, Role.FACULTY)));
        when(studyMaterialRepository.findById(30L)).thenReturn(Optional.of(material));
        when(subjectService.getByStudent(40L)).thenReturn(List.of(subject(21L, user(10L, Role.FACULTY))));

        assertThatThrownBy(() -> service.download(30L, student))
                .isInstanceOf(AccessDeniedException.class);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setRole(role);
        return user;
    }

    private Subject subject(Long id, User faculty) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Subject " + id);
        subject.setCode("S" + id);
        subject.setFaculty(faculty);
        return subject;
    }
}
