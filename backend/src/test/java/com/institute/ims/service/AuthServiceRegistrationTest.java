package com.institute.ims.service;

import com.institute.ims.dto.RegisterRequest;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import com.institute.ims.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    @Test
    void studentCanRegisterWithoutAcademicDetailsOrSubjectChoices() {
        Department department = new Department();
        department.setId(10L);
        when(userRepository.existsByRollNumber("STUDENT")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("student@example.edu")).thenReturn(false);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest();
        request.setName("Student");
        request.setRollNumber("STUDENT");
        request.setPassword("password");
        request.setEmail("student@example.edu");
        request.setRole(Role.STUDENT);
        request.setDepartmentId(10L);

        service.register(request);

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getDepartment()).isSameAs(department);
        assertThat(savedUser.getYear()).isNull();
        assertThat(savedUser.getSemester()).isNull();
        assertThat(savedUser.getSelectedSubjects()).isEmpty();
    }
}
