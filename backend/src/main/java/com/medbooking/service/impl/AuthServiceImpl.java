package com.medbooking.service.impl;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Patient;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.UserRepository;
import com.medbooking.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service thực hiện các nghiệp vụ xác thực, đăng ký và đăng nhập tài khoản.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AuthServiceImpl(UserRepository userRepository, PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Đăng ký tài khoản bệnh nhân mới bao gồm tạo User và hồ sơ Patient.
     */
    @Override
    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest request) {
        // Kiểm tra email đã tồn tại trong hệ thống chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "Email đã được đăng ký trong hệ thống");
        }

        // Tạo tài khoản User mới với vai trò patient
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPassword());
        user.setRole(User.Role.patient);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        // Tạo hồ sơ bệnh nhân liên kết với User ID
        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setFullName(request.getFullName());
        patient.setDob(request.getDob());
        patient.setGender(Patient.Gender.valueOf(request.getGender().toLowerCase()));
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patient.setBloodType(request.getBloodType());

        patientRepository.save(patient);

        // Trả về AuthResponse
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                patient.getFullName(),
                "mock-jwt-token-for-" + savedUser.getId()
        );
    }

    /**
     * Đăng nhập tài khoản và trả về thông tin profile cùng token.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        // Tìm tài khoản theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(401, "Email hoặc mật khẩu không chính xác"));

        // Kiểm tra mật khẩu
        if (!user.getPasswordHash().equals(request.getPassword())) {
            throw new BusinessException(401, "Email hoặc mật khẩu không chính xác");
        }

        // Kiểm tra trạng thái tài khoản
        if (!user.getIsActive()) {
            throw new BusinessException(403, "Tài khoản hiện đang bị khóa");
        }

        // Lấy họ tên hiển thị theo vai trò (Bệnh nhân / Bác sĩ)
        String fullName = "User";
        if (user.getRole() == User.Role.patient) {
            fullName = patientRepository.findById(user.getId()).map(Patient::getFullName).orElse("Bệnh nhân");
        } else if (user.getRole() == User.Role.doctor) {
            fullName = doctorRepository.findById(user.getId()).map(Doctor::getFullName).orElse("Bác sĩ");
        }

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                fullName,
                "mock-jwt-token-for-" + user.getId()
        );
    }
}
