package com.medbooking.service;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.request.VerifyEmailRequest;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.entity.Patient;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.UserRepository;
import com.medbooking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Đăng ký tạo tài khoản chưa xác minh và gửi OTP")
    void registerPatientCreatesPendingAccount() {
        RegisterPatientRequest request = createRegisterRequest();
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10);
            return user;
        });

        AuthResponse response = authService.registerPatient(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertFalse(createdUser.getEmailVerified());
        assertFalse(createdUser.getIsActive());
        assertNull(response.getToken());
        assertEquals("test@gmail.com", response.getEmail());
        verify(emailVerificationService).sendInitialOtp(createdUser, "Test User");
    }

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản đã xác minh")
    void loginSuccessForVerifiedAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("USER@gmail.com ");
        request.setPassword("pass");

        User user = createUser("user@gmail.com", "pass", true);
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));

        Patient patient = new Patient();
        patient.setFullName("Trần Văn C");
        when(patientRepository.findById(5)).thenReturn(Optional.of(patient));

        AuthResponse response = authService.login(request);

        assertEquals(5, response.getUserId());
        assertEquals("Trần Văn C", response.getFullName());
        assertNotNull(response.getToken());
    }

    @Test
    @DisplayName("Không cho đăng nhập khi email chưa xác minh")
    void loginRejectsUnverifiedAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("pass");

        User user = createUser("user@gmail.com", "pass", false);
        user.setIsActive(false);
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(403, exception.getStatus());
        assertEquals("Vui lòng xác minh email trước khi đăng nhập.", exception.getMessage());
    }

    @Test
    @DisplayName("API OTP không cấp token cho tài khoản đã xác minh")
    void verifyEmailRejectsAlreadyVerifiedAccount() {
        User user = createUser("user@gmail.com", "pass", true);
        when(userRepository.findByEmailForUpdate("user@gmail.com")).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("user@gmail.com", "123456"))
        );

        assertEquals(400, exception.getStatus());
        assertEquals("Tài khoản này đã được xác minh", exception.getMessage());
    }
    @Test
    @DisplayName("Xác minh đúng OTP sẽ kích hoạt tài khoản và xóa OTP")
    void verifyEmailSuccess() {
        User user = createUser("user@gmail.com", "pass", false);
        user.setIsActive(false);
        user.setEmailVerificationOtpHash(BCrypt.hashpw("123456", BCrypt.gensalt(4)));
        user.setEmailVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        user.setOtpResendCount(2);
        user.setOtpResendWindowStart(LocalDateTime.now());
        when(userRepository.findByEmailForUpdate("user@gmail.com")).thenReturn(Optional.of(user));

        Patient patient = new Patient();
        patient.setFullName("Test User");
        when(patientRepository.findById(5)).thenReturn(Optional.of(patient));

        AuthResponse response = authService.verifyEmail(new VerifyEmailRequest("user@gmail.com", "123456"));

        assertTrue(user.getEmailVerified());
        assertTrue(user.getIsActive());
        assertNull(user.getEmailVerificationOtpHash());
        assertNull(user.getEmailVerificationOtpExpiresAt());
        assertEquals(0, user.getOtpResendCount());
        assertNull(user.getOtpResendWindowStart());
        assertNotNull(response.getToken());
    }

    @Test
    @DisplayName("OTP hết hạn bị xóa nhưng không reset giới hạn gửi lại trong giờ")
    void expiredOtpKeepsResendWindow() {
        User user = createUser("user@gmail.com", "pass", false);
        user.setEmailVerificationOtpHash(BCrypt.hashpw("123456", BCrypt.gensalt(4)));
        user.setEmailVerificationOtpExpiresAt(LocalDateTime.now().minusSeconds(1));
        user.setOtpResendCount(4);
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(20);
        user.setOtpResendWindowStart(windowStart);
        when(userRepository.findByEmailForUpdate("user@gmail.com")).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("user@gmail.com", "123456"))
        );

        assertEquals(400, exception.getStatus());
        assertNull(user.getEmailVerificationOtpHash());
        assertEquals(4, user.getOtpResendCount());
        assertEquals(windowStart, user.getOtpResendWindowStart());
    }

    private RegisterPatientRequest createRegisterRequest() {
        RegisterPatientRequest request = new RegisterPatientRequest();
        request.setEmail(" TEST@gmail.com ");
        request.setPassword("123456");
        request.setFullName("Test User");
        request.setGender("male");
        request.setPhone("0901234567");
        return request;
    }

    private User createUser(String email, String password, boolean verified) {
        User user = new User();
        user.setId(5);
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(4)));
        user.setRole(User.Role.patient);
        user.setIsActive(true);
        user.setEmailVerified(verified);
        return user;
    }
}
