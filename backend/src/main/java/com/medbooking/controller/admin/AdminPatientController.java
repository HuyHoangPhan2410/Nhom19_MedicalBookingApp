package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.entity.Patient;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/patients")
public class AdminPatientController {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public AdminPatientController(PatientRepository patientRepository, UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<Patient>> getAllPatients() {
        return ApiResponse.success(patientRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Patient> getPatientById(@PathVariable Integer id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bệnh nhân không tồn tại"));
        return ApiResponse.success(patient);
    }

    @PostMapping
    @Transactional
    public ApiResponse<Patient> createPatient(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(400, "Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(body.getOrDefault("password", "password123"), BCrypt.gensalt(10)));
        user.setRole(User.Role.patient);
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setFullName(body.get("fullName"));
        patient.setDob(LocalDate.parse(body.get("dob")));
        patient.setGender(Patient.Gender.valueOf(body.getOrDefault("gender", "male")));
        patient.setPhone(body.get("phone"));
        patient.setAddress(body.get("address"));
        patient.setBloodType(body.get("bloodType"));

        return ApiResponse.success("Tạo bệnh nhân thành công", patientRepository.save(patient));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Patient> updatePatient(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bệnh nhân không tồn tại"));

        if (body.containsKey("fullName")) patient.setFullName(body.get("fullName"));
        if (body.containsKey("dob")) patient.setDob(LocalDate.parse(body.get("dob")));
        if (body.containsKey("gender")) patient.setGender(Patient.Gender.valueOf(body.get("gender")));
        if (body.containsKey("phone")) patient.setPhone(body.get("phone"));
        if (body.containsKey("address")) patient.setAddress(body.get("address"));
        if (body.containsKey("bloodType")) patient.setBloodType(body.get("bloodType"));

        return ApiResponse.success("Cập nhật bệnh nhân thành công", patientRepository.save(patient));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> deletePatient(@PathVariable Integer id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bệnh nhân không tồn tại"));
        User user = patient.getUser();
        patientRepository.delete(patient);
        if (user != null) userRepository.delete(user);
        return ApiResponse.success("Xóa bệnh nhân thành công", null);
    }
}