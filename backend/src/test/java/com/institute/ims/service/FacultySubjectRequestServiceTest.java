package com.institute.ims.service;

import com.institute.ims.common.ConflictException;
import com.institute.ims.dto.SubjectFacultyRequestResponse;
import com.institute.ims.dto.SubjectSummaryResponse;
import com.institute.ims.model.*;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.FacultySubjectRequestRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultySubjectRequestServiceTest {

    @Mock
    private FacultySubjectRequestRepository requestRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;

    private FacultySubjectRequestService service;

    @BeforeEach
    void setUp() {
        service = new FacultySubjectRequestService(
                requestRepository, subjectRepository, userRepository, departmentRepository);
    }

    @Test
    void facultyMayRequestSubjectFromAnyRegisteredDepartment() {
        User faculty = user(1L, Role.FACULTY);
        Department registeredDepartment = department(20L);
        registeredDepartment.setHod(user(10L, Role.HOD));
        faculty.setDepartments(new HashSet<>(Set.of(registeredDepartment)));
        Subject subject = subject(30L, registeredDepartment);
        when(userRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(requestRepository.findFirstByFacultyIdAndSubjectIdAndStatusOrderByCreatedAtDesc(
                1L, 30L, FacultySubjectRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectFacultyRequestResponse response = service.submitRequest(faculty, 30L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.facultyId()).isEqualTo(1L);
        assertThat(response.subjectId()).isEqualTo(30L);
        assertThat(response.departmentId()).isEqualTo(20L);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void eligibleSubjectListIncludesEveryDepartment() {
        User faculty = user(1L, Role.FACULTY);
        Department firstDepartment = department(20L);
        Department secondDepartment = department(21L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findAll()).thenReturn(List.of(
                subject(30L, firstDepartment),
                subject(31L, secondDepartment)));

        var result = service.getEligibleSubjects(faculty);

        assertThat(result).extracting(item -> item.departmentId()).containsExactly(20L, 21L);
    }

    @Test
    void facultyCanRequestSubjectOutsideRegisteredDepartmentsAndRouteUsesSubjectDepartment() {
        User faculty = user(1L, Role.FACULTY);
        Department registeredDepartment = department(20L);
        Department subjectDepartment = department(21L);
        subjectDepartment.setHod(user(11L, Role.HOD));
        faculty.setDepartments(new HashSet<>(Set.of(registeredDepartment)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, subjectDepartment)));
        when(requestRepository.findFirstByFacultyIdAndSubjectIdAndStatusOrderByCreatedAtDesc(
                1L, 30L, FacultySubjectRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectFacultyRequestResponse response = service.submitRequest(faculty, 30L);

        assertThat(response.departmentId()).isEqualTo(21L);
        assertThat(response.departmentName()).isEqualTo("Department 21");
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        User faculty = user(1L, Role.FACULTY);
        Department department = department(20L);
        department.setHod(user(10L, Role.HOD));
        faculty.setDepartments(new HashSet<>(Set.of(department)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, department)));
        when(requestRepository.findFirstByFacultyIdAndSubjectIdAndStatusOrderByCreatedAtDesc(
                1L, 30L, FacultySubjectRequestStatus.PENDING))
                .thenReturn(Optional.of(new FacultySubjectRequest()));

        assertThatThrownBy(() -> service.submitRequest(faculty, 30L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pending request");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void unverifiedFacultyCannotSubmitRequests() {
        User faculty = user(1L, Role.FACULTY);
        faculty.setStatus(VerificationStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(faculty));

        assertThatThrownBy(() -> service.submitRequest(faculty, 30L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("verified faculty");

        verify(subjectRepository, never()).findById(any());
    }

    @Test
    void facultyCannotSubmitRequestWhenSubjectDepartmentHasNoHod() {
        User faculty = user(1L, Role.FACULTY);
        Department department = department(20L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, department)));

        assertThatThrownBy(() -> service.submitRequest(faculty, 30L))
                .isInstanceOf(com.institute.ims.common.ConflictException.class)
                .hasMessageContaining("No HOD");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void hodReceivesOnlyRequestsForTheirAppointedDepartments() {
        User hod = user(10L, Role.HOD);
        User faculty = user(1L, Role.FACULTY);
        Department managedDepartment = department(20L);
        Department otherDepartment = department(21L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(managedDepartment));
        when(requestRepository.findByOrderByCreatedAtDesc()).thenReturn(List.of(
                request(faculty, subject(30L, managedDepartment)),
                request(faculty, subject(31L, otherDepartment))));

        List<SubjectFacultyRequestResponse> result = service.getHodRequests(hod, null, null);

        assertThat(result).extracting(SubjectFacultyRequestResponse::subjectId).containsExactly(30L);
        assertThat(result.get(0).departmentId()).isEqualTo(20L);
    }

    @Test
    void hodCanSelectSubjectsFromEveryDepartmentAndSemesterForAllocation() {
        User hod = user(10L, Role.HOD);
        Subject subjectInOtherDepartment = subject(31L, department(21L));
        subjectInOtherDepartment.setSemester(8);
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));
        when(subjectRepository.findAll()).thenReturn(List.of(
                subject(30L, department(20L)),
                subjectInOtherDepartment));

        List<SubjectSummaryResponse> result = service.getHodSubjects(hod);

        assertThat(result).extracting(SubjectSummaryResponse::id).containsExactly(30L, 31L);
    }

    @Test
    void approvalCreatesTeachingLinkAndRetainsReviewerAndOutcome() {
        User hod = user(10L, Role.HOD);
        User faculty = user(1L, Role.FACULTY);
        Department department = department(20L);
        hod.setDepartments(new HashSet<>(Set.of(department)));
        faculty.setDepartments(new HashSet<>(Set.of(department(21L))));
        Subject subject = subject(30L, department);
        FacultySubjectRequest request = new FacultySubjectRequest();
        request.setFaculty(faculty);
        request.setSubject(subject);
        request.setStatus(FacultySubjectRequestStatus.PENDING);
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(department));
        when(requestRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectFacultyRequestResponse response = service.decide(
                hod, 40L, FacultySubjectRequestStatus.APPROVED);

        assertThat(subject.getFacultyMembers()).contains(faculty);
        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(request.getReviewedBy()).isSameAs(hod);
        assertThat(request.getReviewedAt()).isNotNull();
        assertThat(faculty.getDepartments()).extracting(Department::getId).containsExactlyInAnyOrder(20L, 21L);
        assertThat(faculty.getDepartment()).isSameAs(department);
        verify(userRepository).save(faculty);
        verify(subjectRepository).save(subject);
    }

    @Test
    void alreadyDecidedRequestCannotBeChangedAgain() {
        User hod = user(10L, Role.HOD);
        Department department = department(20L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(department));
        FacultySubjectRequest request = new FacultySubjectRequest();
        request.setSubject(subject(30L, department));
        request.setStatus(FacultySubjectRequestStatus.REJECTED);
        when(requestRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.decide(hod, 40L, FacultySubjectRequestStatus.APPROVED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been decided");

        verify(subjectRepository, never()).save(any());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void hodCannotDecideRequestForDepartmentOutsideTheirAppointment() {
        User hod = user(10L, Role.HOD);
        Department otherDepartment = department(21L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(department(20L)));
        FacultySubjectRequest request = new FacultySubjectRequest();
        request.setSubject(subject(30L, otherDepartment));
        request.setStatus(FacultySubjectRequestStatus.PENDING);
        when(requestRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.decide(hod, 40L, FacultySubjectRequestStatus.REJECTED))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("appointed departments");

        verify(requestRepository, never()).save(any());
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(VerificationStatus.VERIFIED);
        user.setName("User " + id);
        user.setRollNumber("U" + id);
        return user;
    }

    private Department department(Long id) {
        Department department = new Department();
        department.setId(id);
        department.setName("Department " + id);
        return department;
    }

    private Subject subject(Long id, Department department) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Subject " + id);
        subject.setCode("S" + id);
        subject.setDepartment(department);
        subject.setSemester(1);
        return subject;
    }

    private FacultySubjectRequest request(User faculty, Subject subject) {
        FacultySubjectRequest request = new FacultySubjectRequest();
        request.setFaculty(faculty);
        request.setSubject(subject);
        request.setStatus(FacultySubjectRequestStatus.PENDING);
        return request;
    }
}
