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
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "Email đã được đăng ký trong hệ thống");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        // ✅ BCrypt hash thay vì plaintext
        user.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(10)));
        user.setRole(User.Role.patient);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setFullName(request.getFullName());
        patient.setDob(request.getDob());
        patient.setGender(Patient.Gender.valueOf(request.getGender().toLowerCase()));
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patient.setBloodType(request.getBloodType());

        patientRepository.save(patient);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                patient.getFullName(),
                "mock-jwt-token-for-" + savedUser.getId()
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(401, "Email hoặc mật khẩu không chính xác"));

        // ✅ BCrypt check thay vì equals()
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "Email hoặc mật khẩu không chính xác");
        }

        if (!user.getIsActive()) {
            throw new BusinessException(403, "Tài khoản hiện đang bị khóa");
        }

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