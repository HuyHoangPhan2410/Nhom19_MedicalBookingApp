package com.medbooking.service;

import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {

    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_RESEND_PER_HOUR = 5;

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    public EmailVerificationService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    public void sendInitialOtp(User user, String fullName) {
        user.setOtpResendCount(0);
        user.setOtpResendWindowStart(LocalDateTime.now());
        createOtpAndSendEmail(user, fullName);
    }

    public void resendOtp(User user, String fullName) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = user.getOtpResendWindowStart();

        if (windowStart == null || windowStart.isBefore(now.minusHours(1))) {
            user.setOtpResendWindowStart(now);
            user.setOtpResendCount(0);
        }

        int currentCount = user.getOtpResendCount() == null ? 0 : user.getOtpResendCount();
        if (currentCount >= MAX_RESEND_PER_HOUR) {
            throw new BusinessException(429, "B\u1ea1n \u0111\u00e3 g\u1eedi l\u1ea1i OTP qu\u00e1 5 l\u1ea7n trong 1 gi\u1edd. Vui l\u00f2ng th\u1eed l\u1ea1i sau.");
        }

        user.setOtpResendCount(currentCount + 1);
        createOtpAndSendEmail(user, fullName);
    }

    private void createOtpAndSendEmail(User user, String fullName) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setEmailVerificationOtpHash(BCrypt.hashpw(otp, BCrypt.gensalt(10)));
        user.setEmailVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        userRepository.save(user);

        sendOtpEmail(user.getEmail(), fullName, otp);
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp) {
        if (!StringUtils.hasText(mailUsername)) {
            throw new BusinessException(500, "Ch\u01b0a c\u1ea5u h\u00ecnh Gmail SMTP. Vui l\u00f2ng c\u1ea5u h\u00ecnh SPRING_MAIL_USERNAME v\u00e0 SPRING_MAIL_PASSWORD.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Medical Booking - X\u00e1c minh t\u00e0i kho\u1ea3n");
            helper.setFrom(StringUtils.hasText(mailFrom) ? mailFrom : mailUsername);
            helper.setText(buildEmailHtml(fullName, otp), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new BusinessException(500, "Kh\u00f4ng th\u1ec3 t\u1ea1o email x\u00e1c minh. Vui l\u00f2ng th\u1eed l\u1ea1i sau.");
        } catch (Exception ex) {
            throw new BusinessException(500, "Kh\u00f4ng th\u1ec3 g\u1eedi OTP qua email. Vui l\u00f2ng ki\u1ec3m tra c\u1ea5u h\u00ecnh Gmail SMTP.");
        }
    }

    private String buildEmailHtml(String fullName, String otp) {
        String safeName = escapeHtml(StringUtils.hasText(fullName) ? fullName : "b\u1ea1n");
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#0f172a">
                  <p>Xin ch\u00e0o <strong>%s</strong>,</p>
                  <p>C\u1ea3m \u01a1n b\u1ea1n \u0111\u00e3 \u0111\u0103ng k\u00fd t\u00e0i kho\u1ea3n Medical Booking.</p>
                  <p>M\u00e3 OTP x\u00e1c minh c\u1ee7a b\u1ea1n l\u00e0:</p>
                  <div style="font-size:28px;font-weight:700;letter-spacing:6px;margin:16px 0;color:#0f766e">%s</div>
                  <p>M\u00e3 c\u00f3 hi\u1ec7u l\u1ef1c trong 5 ph\u00fat.</p>
                  <p>N\u1ebfu b\u1ea1n kh\u00f4ng th\u1ef1c hi\u1ec7n \u0111\u0103ng k\u00fd t\u00e0i kho\u1ea3n, vui l\u00f2ng b\u1ecf qua email n\u00e0y.</p>
                </div>
                """.formatted(safeName, otp);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
