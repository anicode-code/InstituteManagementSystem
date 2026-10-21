package com.institute.ims;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institute.ims.dto.LoginRequest;
import com.institute.ims.dto.RegisterRequest;
import com.institute.ims.dto.VerifyBulkRequest;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ImsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_ADMIN_ROLL = "TEST_ADMIN_001";
    private static final String TEST_STUDENT_ROLL = "TEST_STU_001";
    private static final String TEST_PASSWORD = "testpass123";

    @BeforeEach
    void setUp() {
        // Clean up test users before each test
        userRepository.findByRollNumber(TEST_ADMIN_ROLL).ifPresent(userRepository::delete);
        userRepository.findByRollNumber(TEST_STUDENT_ROLL).ifPresent(userRepository::delete);

        // Create a verified ADMIN test user
        User admin = new User();
        admin.setName("Test Admin");
        admin.setRollNumber(TEST_ADMIN_ROLL);
        admin.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        admin.setEmail("testadmin@test.com");
        admin.setRole(Role.ADMIN);
        admin.setStatus(VerificationStatus.VERIFIED);
        userRepository.save(admin);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Context load
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void contextLoads() {
    }

    // ─────────────────────────────────────────────────────────────────────
    // Registration tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void registration_validRollNumberAndPassword_shouldSucceed() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("New Student");
        req.setRollNumber("2026CS999");
        req.setPassword("pass1234");
        req.setEmail("newstudent999@test.com");
        req.setRole(Role.STUDENT);

        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rollNumber").value("2026CS999"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        } finally {
            userRepository.findByRollNumber("2026CS999").ifPresent(userRepository::delete);
        }
    }

    @Test
    void registration_duplicateRollNumber_shouldFail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Dup Student");
        req.setRollNumber(TEST_ADMIN_ROLL); // Already exists
        req.setPassword("pass1234");
        req.setEmail("dup@test.com");
        req.setRole(Role.STUDENT);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registration_newUserShouldBePending() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Pending Test");
        req.setRollNumber("2026PENDING01");
        req.setPassword("pass1234");
        req.setEmail("pending01@test.com");
        req.setRole(Role.STUDENT);

        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));

            User saved = userRepository.findByRollNumber("2026PENDING01").orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
        } finally {
            userRepository.findByRollNumber("2026PENDING01").ifPresent(userRepository::delete);
        }
    }

    @Test
    void registration_rollNumberIsString_preserved() throws Exception {
        // Verifies leading zeros and string format preserved
        RegisterRequest req = new RegisterRequest();
        req.setName("Zero Student");
        req.setRollNumber("0026CS001");
        req.setPassword("pass1234");
        req.setEmail("zero001@test.com");
        req.setRole(Role.STUDENT);

        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rollNumber").value("0026CS001"));
        } finally {
            userRepository.findByRollNumber("0026CS001").ifPresent(userRepository::delete);
        }
    }

    @Test
    void registration_missingRollNumber_shouldFail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("No Roll");
        req.setRollNumber(""); // blank
        req.setPassword("pass1234");
        req.setEmail("noroll@test.com");
        req.setRole(Role.STUDENT);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Login tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void login_validRollNumberAndPassword_shouldReturnToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setRollNumber(TEST_ADMIN_ROLL);
        req.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rollNumber").value(TEST_ADMIN_ROLL));
    }

    @Test
    void login_invalidPassword_shouldFail() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setRollNumber(TEST_ADMIN_ROLL);
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_nonExistentRollNumber_shouldFail() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setRollNumber("DOES_NOT_EXIST");
        req.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helper: get JWT token for admin
    // ─────────────────────────────────────────────────────────────────────
    private String loginAndGetToken(String rollNumber, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setRollNumber(rollNumber);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Admin verification tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void adminCanViewPendingUsers() throws Exception {
        // Register a pending user
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Pending View");
        reg.setRollNumber("2026VIEWTEST");
        reg.setPassword("pass1234");
        reg.setEmail("viewtest@test.com");
        reg.setRole(Role.STUDENT);

        try {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reg)));

            String token = loginAndGetToken(TEST_ADMIN_ROLL, TEST_PASSWORD);

            mockMvc.perform(get("/api/users/pending")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        } finally {
            userRepository.findByRollNumber("2026VIEWTEST").ifPresent(userRepository::delete);
        }
    }

    @Test
    void adminCanVerifyPendingUser() throws Exception {
        // Register a pending user
        User pending = new User();
        pending.setName("Pending Verify");
        pending.setRollNumber("2026VERIFYTEST");
        pending.setPassword(passwordEncoder.encode("pass1234"));
        pending.setEmail("verifytest@test.com");
        pending.setRole(Role.STUDENT);
        pending.setStatus(VerificationStatus.PENDING);
        pending = userRepository.save(pending);
        Long pendingId = pending.getId();

        try {
            String token = loginAndGetToken(TEST_ADMIN_ROLL, TEST_PASSWORD);

            mockMvc.perform(put("/api/users/" + pendingId + "/verify")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VERIFIED"));

            User verified = userRepository.findById(pendingId).orElseThrow();
            assertThat(verified.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        } finally {
            userRepository.findByRollNumber("2026VERIFYTEST").ifPresent(userRepository::delete);
        }
    }

    @Test
    void adminCanBulkVerifyUsers() throws Exception {
        User p1 = new User();
        p1.setName("Bulk One");
        p1.setRollNumber("2026BULK001");
        p1.setPassword(passwordEncoder.encode("pass1234"));
        p1.setEmail("bulk001@test.com");
        p1.setRole(Role.STUDENT);
        p1.setStatus(VerificationStatus.PENDING);
        p1 = userRepository.save(p1);

        User p2 = new User();
        p2.setName("Bulk Two");
        p2.setRollNumber("2026BULK002");
        p2.setPassword(passwordEncoder.encode("pass1234"));
        p2.setEmail("bulk002@test.com");
        p2.setRole(Role.STUDENT);
        p2.setStatus(VerificationStatus.PENDING);
        p2 = userRepository.save(p2);

        try {
            String token = loginAndGetToken(TEST_ADMIN_ROLL, TEST_PASSWORD);
            VerifyBulkRequest req = new VerifyBulkRequest(List.of(p1.getId(), p2.getId()));

            mockMvc.perform(post("/api/users/verify-bulk")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verifiedCount").value(2));

            assertThat(userRepository.findById(p1.getId()).orElseThrow().getStatus())
                    .isEqualTo(VerificationStatus.VERIFIED);
            assertThat(userRepository.findById(p2.getId()).orElseThrow().getStatus())
                    .isEqualTo(VerificationStatus.VERIFIED);
        } finally {
            userRepository.findByRollNumber("2026BULK001").ifPresent(userRepository::delete);
            userRepository.findByRollNumber("2026BULK002").ifPresent(userRepository::delete);
        }
    }

    @Test
    void bulkVerifySelectAllThenUnselectSome_onlySelectedVerified() throws Exception {
        // Simulate: select all 3, then exclude p3 → only p1 and p2 should be verified
        User p1 = new User(); p1.setName("Sel1"); p1.setRollNumber("2026SEL001");
        p1.setPassword(passwordEncoder.encode("pass")); p1.setEmail("sel001@test.com");
        p1.setRole(Role.STUDENT); p1.setStatus(VerificationStatus.PENDING);
        p1 = userRepository.save(p1);

        User p2 = new User(); p2.setName("Sel2"); p2.setRollNumber("2026SEL002");
        p2.setPassword(passwordEncoder.encode("pass")); p2.setEmail("sel002@test.com");
        p2.setRole(Role.STUDENT); p2.setStatus(VerificationStatus.PENDING);
        p2 = userRepository.save(p2);

        User p3 = new User(); p3.setName("Sel3"); p3.setRollNumber("2026SEL003");
        p3.setPassword(passwordEncoder.encode("pass")); p3.setEmail("sel003@test.com");
        p3.setRole(Role.STUDENT); p3.setStatus(VerificationStatus.PENDING);
        p3 = userRepository.save(p3);

        try {
            String token = loginAndGetToken(TEST_ADMIN_ROLL, TEST_PASSWORD);
            // Only verify p1 and p2 (p3 was deselected)
            VerifyBulkRequest req = new VerifyBulkRequest(List.of(p1.getId(), p2.getId()));

            mockMvc.perform(post("/api/users/verify-bulk")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verifiedCount").value(2));

            assertThat(userRepository.findById(p1.getId()).orElseThrow().getStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(userRepository.findById(p2.getId()).orElseThrow().getStatus()).isEqualTo(VerificationStatus.VERIFIED);
            // p3 should still be PENDING
            assertThat(userRepository.findById(p3.getId()).orElseThrow().getStatus()).isEqualTo(VerificationStatus.PENDING);
        } finally {
            userRepository.findByRollNumber("2026SEL001").ifPresent(userRepository::delete);
            userRepository.findByRollNumber("2026SEL002").ifPresent(userRepository::delete);
            userRepository.findByRollNumber("2026SEL003").ifPresent(userRepository::delete);
        }
    }

    @Test
    void bulkVerify_emptyList_shouldThrowError() throws Exception {
        String token = loginAndGetToken(TEST_ADMIN_ROLL, TEST_PASSWORD);
        VerifyBulkRequest req = new VerifyBulkRequest(List.of());

        mockMvc.perform(post("/api/users/verify-bulk")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotVerifyUsers() throws Exception {
        // Create a non-admin pending user, try to verify someone as student
        User student = new User();
        student.setName("Student Actor");
        student.setRollNumber("2026STUACTOR");
        student.setPassword(passwordEncoder.encode("pass1234"));
        student.setEmail("stuactor@test.com");
        student.setRole(Role.STUDENT);
        student.setStatus(VerificationStatus.VERIFIED);
        student = userRepository.save(student);

        User target = new User();
        target.setName("Target Pending");
        target.setRollNumber("2026TARGET");
        target.setPassword(passwordEncoder.encode("pass1234"));
        target.setEmail("target@test.com");
        target.setRole(Role.STUDENT);
        target.setStatus(VerificationStatus.PENDING);
        target = userRepository.save(target);
        Long targetId = target.getId();

        try {
            String token = loginAndGetToken("2026STUACTOR", "pass1234");

            mockMvc.perform(put("/api/users/" + targetId + "/verify")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        } finally {
            userRepository.findByRollNumber("2026STUACTOR").ifPresent(userRepository::delete);
            userRepository.findByRollNumber("2026TARGET").ifPresent(userRepository::delete);
        }
    }

    @Test
    void userCannotVerifyThemselves() throws Exception {
        // Admin tries to verify their own account
        String token = loginAndGetToken(TEST_ADMIN_ROLL, TEST_PASSWORD);
        Long adminId = userRepository.findByRollNumber(TEST_ADMIN_ROLL).orElseThrow().getId();

        mockMvc.perform(put("/api/users/" + adminId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("cannot verify themselves")));
    }

    @Test
    void login_unverifiedUser_shouldFail() throws Exception {
        User pending = new User();
        pending.setName("Unverified Student");
        pending.setRollNumber("2026UNVERIFIED");
        pending.setPassword(passwordEncoder.encode("secret123"));
        pending.setEmail("unverified@test.com");
        pending.setRole(Role.STUDENT);
        pending.setStatus(VerificationStatus.PENDING);
        userRepository.save(pending);

        try {
            LoginRequest req = new LoginRequest();
            req.setRollNumber("2026UNVERIFIED");
            req.setPassword("secret123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("not been verified")));
        } finally {
            userRepository.findByRollNumber("2026UNVERIFIED").ifPresent(userRepository::delete);
        }
    }

    @Test
    void departments_unauthenticatedGet_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
