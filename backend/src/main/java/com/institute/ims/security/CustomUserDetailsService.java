package com.institute.ims.security;

import com.institute.ims.model.User;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String rollNumber)
            throws UsernameNotFoundException {

        if (rollNumber == null) {
            throw new UsernameNotFoundException("Roll number cannot be null");
        }

        User user =
                userRepository.findByRollNumber(rollNumber.trim())
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found with roll number: " + rollNumber));

        return new org.springframework.security.core.userdetails.User(
                user.getRollNumber(),
                user.getPassword(),
                user.getStatus() == com.institute.ims.model.VerificationStatus.VERIFIED,
                true,
                true,
                true,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                )
        );
    }
}