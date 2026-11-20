package com.institute.ims.service;

import com.institute.ims.dto.RegisterRequest;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import com.institute.ims.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @Test
    void facultyRegistrationDefaultsAdministrativeRoleToNoneAndNoDepartment() {
        when(userRepository.existsByRollNumber("FAC001")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("faculty@example.edu")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest();
        request.setName("Faculty Member");
        request.setRollNumber("FAC001");
        request.setPassword("password");
        request.setEmail("faculty@example.edu");
        request.setRole(Role.FACULTY);

        AuthService service = new AuthService(
                authenticationManager,
                userRepository,
                departmentRepository,
                userDetailsService,
                passwordEncoder,
                jwtService);

        service.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getAdministrativeRole()).isEqualTo("NONE");
        assertThat(savedUser.getValue().getDepartment()).isNull();
        assertThat(savedUser.getValue().getDepartments()).isEmpty();
        verifyNoInteractions(departmentRepository);
    }
}
