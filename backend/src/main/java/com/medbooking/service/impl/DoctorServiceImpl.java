package com.medbooking.service.impl;

import com.medbooking.dto.request.RegisterDoctorRequest;
import com.medbooking.dto.response.DoctorResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.service.DoctorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service quản lý thông tin và hồ sơ bác sĩ.
 */
@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /**
     * Lấy danh sách tất cả bác sĩ trong hệ thống.
     */
    @Override
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết hồ sơ bác sĩ theo ID (user_id).
     */
    @Override
    public DoctorResponse getDoctorById(Integer id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Bác sĩ không tồn tại"));
        return mapToResponse(doctor);
    }

    /**
     * Lấy danh sách bác sĩ thuộc về một chuyên khoa.
     */
    @Override
    public List<DoctorResponse> getDoctorsBySpecialty(Integer specialtyId) {
        return doctorRepository.findBySpecialtyId(specialtyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đăng ký hồ sơ bác sĩ mới qua Stored Procedure sp_RegisterDoctor.
     */
    @Override
    @Transactional
    public DoctorResponse registerDoctor(RegisterDoctorRequest request) {
        // Gọi Stored Procedure dưới MySQL
        Map<String, Object> resultMap = doctorRepository.spRegisterDoctor(
                request.getEmail(),
                request.getPassword(),
                request.getSpecialtyId(),
                request.getFullName(),
                request.getExperienceYears(),
                request.getConsultationFee(),
                request.getBio()
        );

        Integer doctorId = (Integer) resultMap.get("p_doctor_id");
        String message = (String) resultMap.get("p_message");

        if (doctorId == null) {
            throw new BusinessException(400, message != null ? message : "Đăng ký bác sĩ thất bại");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy hồ sơ bác sĩ sau khi tạo"));

        return mapToResponse(doctor);
    }

    /**
     * Chuyển đổi Doctor Entity sang DTO DoctorResponse.
     */
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
