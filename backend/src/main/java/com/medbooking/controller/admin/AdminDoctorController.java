package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.DoctorResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Specialty;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.SpecialtyRepository;
import com.medbooking.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/doctors")
public class AdminDoctorController {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;

    public AdminDoctorController(DoctorRepository doctorRepository,
                                  UserRepository userRepository,
                                  SpecialtyRepository specialtyRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    public ApiResponse<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> list = doctorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorResponse> getDoctorById(@PathVariable Integer id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bác sĩ không tồn tại"));
        return ApiResponse.success(mapToResponse(doctor));
    }

    @PostMapping
    @Transactional
    public ApiResponse<DoctorResponse> createDoctor(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(400, "Email đã tồn tại");
        }

        Integer specialtyId = Integer.parseInt(body.get("specialtyId"));
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new BusinessException(404, "Chuyên khoa không tồn tại"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(body.getOrDefault("password", "password123"), BCrypt.gensalt(10)));
        user.setRole(User.Role.doctor);
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setSpecialty(specialty);
        doctor.setFullName(body.get("fullName"));
        doctor.setExperienceYears(Integer.parseInt(body.getOrDefault("experienceYears", "0")));
        doctor.setConsultationFee(new BigDecimal(body.getOrDefault("consultationFee", "0")));
        doctor.setBio(body.get("bio"));

        doctorRepository.save(doctor);
        return ApiResponse.success("Tạo bác sĩ thành công", mapToResponse(doctor));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<DoctorResponse> updateDoctor(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bác sĩ không tồn tại"));

        if (body.containsKey("fullName")) doctor.setFullName(body.get("fullName"));
        if (body.containsKey("experienceYears")) doctor.setExperienceYears(Integer.parseInt(body.get("experienceYears")));
        if (body.containsKey("consultationFee")) doctor.setConsultationFee(new BigDecimal(body.get("consultationFee")));
        if (body.containsKey("bio")) doctor.setBio(body.get("bio"));
        if (body.containsKey("specialtyId")) {
            Specialty specialty = specialtyRepository.findById(Integer.parseInt(body.get("specialtyId")))
                    .orElseThrow(() -> new BusinessException(404, "Chuyên khoa không tồn tại"));
            doctor.setSpecialty(specialty);
        }

        doctorRepository.save(doctor);
        return ApiResponse.success("Cập nhật bác sĩ thành công", mapToResponse(doctor));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> deleteDoctor(@PathVariable Integer id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bác sĩ không tồn tại"));
        User user = doctor.getUser();
        doctorRepository.delete(doctor);
        if (user != null) userRepository.delete(user);
        return ApiResponse.success("Xóa bác sĩ thành công", null);
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        DoctorResponse res = new DoctorResponse();
        res.setUserId(doctor.getUserId());
        res.setFullName(doctor.getFullName());
        res.setSpecialtyId(doctor.getSpecialty().getId());
        res.setSpecialtyName(doctor.getSpecialty().getName());
        res.setExperienceYears(doctor.getExperienceYears());
        res.setConsultationFee(doctor.getConsultationFee());
        res.setBio(doctor.getBio());
        return res;
    }
}