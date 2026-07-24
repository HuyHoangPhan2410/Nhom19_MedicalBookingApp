package com.medbooking.service;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.entity.Patient;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.UserRepository;
import com.medbooking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho chức năng xác thực, đăng ký và đăng nhập tài khoản.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Kiểm thử đăng ký bệnh nhân mới thành công")
    void testRegisterPatient_Success() {
        // 1. Tạo request đăng ký bệnh nhân
        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("123456");
        req.setFullName("Test User");
        req.setGender("male");

        // 2. Giả lập kiểm tra email chưa tồn tại
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);

        // 3. Giả lập lưu User mới
        User savedUser = new User();
        savedUser.setId(10);
        savedUser.setEmail("test@gmail.com");
        savedUser.setRole(User.Role.patient);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 4. Thực thi đăng ký
        AuthResponse res = authService.registerPatient(req);

        // 5. Kiểm tra kết quả
        assertNotNull(res, "Thông tin phản hồi đăng ký không được null");
        assertEquals(10, res.getUserId(), "User ID phải là 10");
        assertEquals("test@gmail.com", res.getEmail(), "Email phải khớp với dữ liệu đăng ký");
    }

    @Test
    @DisplayName("Kiểm thử đăng nhập thành công với thông tin chính xác")
    void testLogin_Success() {
        // 1. Tạo request đăng nhập
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("pass");

        // 2. Giả lập tìm thấy User trong Database
        User user = new User();
        user.setId(5);
        user.setEmail("user@gmail.com");
        user.setPasswordHash("pass");
        user.setRole(User.Role.patient);
        user.setIsActive(true);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));

        // 3. Giả lập tìm thấy thông tin hồ sơ bệnh nhân
        Patient patient = new Patient();
        patient.setFullName("Trần Văn C");
        when(patientRepository.findById(5)).thenReturn(Optional.of(patient));

        // 4. Thực thi đăng nhập
        AuthResponse res = authService.login(req);

        // 5. Kiểm tra kết quả
        assertNotNull(res, "Thông tin phản hồi đăng nhập không được null");
        assertEquals(5, res.getUserId(), "Mã tài khoản phải khớp");
        assertEquals("Trần Văn C", res.getFullName(), "Tên hiển thị phải khớp với hồ sơ bệnh nhân");
    }

    @Test
    @DisplayName("Kiểm thử đăng nhập thất bại khi nhập sai mật khẩu")
    void testLogin_WrongPassword() {
        // 1. Tạo request đăng nhập với mật khẩu sai
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("wrong");

        // 2. Giả lập User có mật khẩu đúng là 'correct'
        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPasswordHash("correct");

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));

        // 3. Xung đột mật khẩu phải ném ra ngoại lệ BusinessException
        assertThrows(BusinessException.class, () -> authService.login(req), "Phải ném ngoại lệ khi mật khẩu sai");
    }
}
