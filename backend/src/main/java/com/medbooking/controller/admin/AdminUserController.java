package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Patient;
import com.medbooking.entity.Specialty;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.SpecialtyRepository;
import com.medbooking.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "Administration", description = "Admin login, dashboard and CRUD APIs")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SpecialtyRepository specialtyRepository;

    public AdminUserController(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            SpecialtyRepository specialtyRepository
    ) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    public ApiResponse<List<User>> getAllUsers() {
        return ApiResponse.success(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User does not exist"));
        return ApiResponse.success(user);
    }

    @PostMapping
    @Transactional
    public ApiResponse<User> createUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        User.Role role = parseRole(body.get("role"));

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "Email and password are required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(400, "Email already exists");
        }

        User user = new User();
        user.setEmail(email.trim());
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);

        if (role == User.Role.doctor) {
            createDoctorProfile(savedUser, body);
        }

        return ApiResponse.success("Account created successfully", savedUser);
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<User> updateUser(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User does not exist"));

        if (body.containsKey("email")) {
            String newEmail = body.get("email");
            if (!StringUtils.hasText(newEmail)) {
                throw new BusinessException(400, "Email is required");
            }
            newEmail = newEmail.trim();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new BusinessException(400, "Email already exists");
            }
            user.setEmail(newEmail);
        }

        User.Role targetRole = body.containsKey("role") ? parseRole(body.get("role")) : user.getRole();
        user.setRole(targetRole);

        if (body.containsKey("isActive")) {
            user.setIsActive(Boolean.parseBoolean(body.get("isActive")));
        }
        if (body.containsKey("password") && StringUtils.hasText(body.get("password"))) {
            user.setPasswordHash(BCrypt.hashpw(body.get("password"), BCrypt.gensalt(10)));
        }

        User savedUser = userRepository.save(user);

        // A doctor role is only usable when the matching Doctors row exists. This also
        // repairs accounts that were converted by the old implementation.
        if (targetRole == User.Role.doctor) {
            Doctor doctor = doctorRepository.findById(id).orElse(null);
            if (doctor == null) {
                createDoctorProfile(savedUser, body);
            } else {
                updateDoctorProfile(doctor, body);
            }
        }

        return ApiResponse.success("Account updated successfully", savedUser);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User does not exist"));
        userRepository.delete(user);
        return ApiResponse.success("Account deleted successfully", null);
    }

    private User.Role parseRole(String role) {
        try {
            return User.Role.valueOf(role);
        } catch (Exception ex) {
            throw new BusinessException(400, "Invalid account role");
        }
    }

    private void createDoctorProfile(User user, Map<String, String> body) {
        Specialty specialty = getSpecialty(body);
        Patient patient = patientRepository.findById(user.getId()).orElse(null);
        String fallbackName = patient != null ? patient.getFullName() : user.getEmail().split("@")[0];

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialty(specialty);
        doctor.setFullName(textOrDefault(body.get("fullName"), fallbackName));
        doctor.setExperienceYears(nonNegativeInteger(body.get("experienceYears"), 0, "Experience years"));
        doctor.setConsultationFee(nonNegativeDecimal(body.get("consultationFee"), BigDecimal.ZERO));
        doctor.setBio(body.get("bio"));
        doctorRepository.save(doctor);
    }

    private void updateDoctorProfile(Doctor doctor, Map<String, String> body) {
        if (body.containsKey("specialtyId")) doctor.setSpecialty(getSpecialty(body));
        if (StringUtils.hasText(body.get("fullName"))) doctor.setFullName(body.get("fullName").trim());
        if (body.containsKey("experienceYears")) {
            doctor.setExperienceYears(nonNegativeInteger(body.get("experienceYears"), 0, "Experience years"));
        }
        if (body.containsKey("consultationFee")) {
            doctor.setConsultationFee(nonNegativeDecimal(body.get("consultationFee"), BigDecimal.ZERO));
        }
        if (body.containsKey("bio")) doctor.setBio(body.get("bio"));
        doctorRepository.save(doctor);
    }

    private Specialty getSpecialty(Map<String, String> body) {
        String specialtyIdValue = body.get("specialtyId");
        if (!StringUtils.hasText(specialtyIdValue)) {
            throw new BusinessException(400, "Specialty is required when changing an account to doctor");
        }
        try {
            Integer specialtyId = Integer.valueOf(specialtyIdValue);
            return specialtyRepository.findById(specialtyId)
                    .orElseThrow(() -> new BusinessException(404, "Specialty does not exist"));
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, "Invalid specialty");
        }
    }

    private int nonNegativeInteger(String value, int defaultValue, String fieldName) {
        if (!StringUtils.hasText(value)) return defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, fieldName + " must be a non-negative number");
        }
    }

    private BigDecimal nonNegativeDecimal(String value, BigDecimal defaultValue) {
        if (!StringUtils.hasText(value)) return defaultValue;
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, "Consultation fee must be a non-negative number");
        }
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
