package com.medbooking.service;

import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(mailSender, userRepository);
    }

    @Test
    @DisplayName("Chặn lần gửi lại OTP thứ 6 trong cùng một giờ")
    void resendOtpRejectsSixthAttemptWithinHour() {
        User user = new User();
        user.setOtpResendCount(5);
        user.setOtpResendWindowStart(LocalDateTime.now().minusMinutes(30));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> emailVerificationService.resendOtp(user, "Test User")
        );

        assertEquals(429, exception.getStatus());
        assertEquals(5, user.getOtpResendCount());
    }
}
