package com.medbooking.service.impl;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.request.ResendOtpRequest;
import com.medbooking.dto.request.VerifyEmailRequest;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Patient;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.UserRepository;
import com.medbooking.service.AuthService;
import com.medbooking.service.EmailVerificationService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final EmailVerificationService emailVerificationService;

    public AuthServiceImpl(UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(400, "Email đã được đăng ký trong hệ thống");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "Mật khẩu không được để trống");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(10)));
        user.setRole(User.Role.patient);
        user.setIsActive(false);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setFullName(request.getFullName());
        patient.setDob(request.getDob());
        patient.setGender(Patient.Gender.valueOf(request.getGender().toLowerCase(Locale.ROOT)));
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patient.setBloodType(request.getBloodType());

        patientRepository.save(patient);
        emailVerificationService.sendInitialOtp(savedUser, patient.getFullName());

        // Registration must not create an authenticated frontend session before verification.
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                patient.getFullName(),
                null
        );
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new BusinessException(404, "Email không tồn tại trong hệ thống"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(400, "Tài khoản này đã được xác minh");
        }

        String otp = request.getOtp() == null ? "" : request.getOtp().trim();
        if (!otp.matches("\\d{6}")) {
            throw new BusinessException(400, "Mã OTP phải gồm 6 chữ số");
        }

        if (user.getEmailVerificationOtpHash() == null || user.getEmailVerificationOtpExpiresAt() == null) {
            throw new BusinessException(400, "Mã OTP không tồn tại. Vui lòng gửi lại OTP.");
        }

        if (!user.getEmailVerificationOtpExpiresAt().isAfter(LocalDateTime.now())) {
            clearOtpValue(user);
            userRepository.save(user);
            throw new BusinessException(400, "Mã OTP đã hết hạn. Vui lòng gửi lại OTP.");
        }

        if (!BCrypt.checkpw(otp, user.getEmailVerificationOtpHash())) {
            throw new BusinessException(400, "Mã OTP không chính xác");
        }

        user.setEmailVerified(true);
        user.setIsActive(true);
        clearOtpValue(user);
        user.setOtpResendCount(0);
        user.setOtpResendWindowStart(null);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new BusinessException(404, "Email không tồn tại trong hệ thống"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(400, "Tài khoản này đã được xác minh");
        }

        emailVerificationService.resendOtp(user, getFullName(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String password = request == null ? null : request.getPassword();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(401, "Email hoặc mật khẩu không chính xác"));

        if (!StringUtils.hasText(password) || !BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new BusinessException(401, "Email hoặc mật khẩu không chính xác");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(403, "Vui lòng xác minh email trước khi đăng nhập.");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException(403, "Tài khoản hiện đang bị khóa");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                getFullName(user),
                "mock-jwt-token-for-" + user.getId()
        );
    }

    private String getFullName(User user) {
        if (user.getRole() == User.Role.patient) {
            return patientRepository.findById(user.getId()).map(Patient::getFullName).orElse("Bệnh nhân");
        }
        if (user.getRole() == User.Role.doctor) {
            return doctorRepository.findById(user.getId()).map(Doctor::getFullName).orElse("Bác sĩ");
        }
        return "User";
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(400, "Email không được để trống");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void clearOtpValue(User user) {
        user.setEmailVerificationOtpHash(null);
        user.setEmailVerificationOtpExpiresAt(null);
    }
}
