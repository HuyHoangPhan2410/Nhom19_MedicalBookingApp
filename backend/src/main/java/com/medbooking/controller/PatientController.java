package com.medbooking.controller;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.entity.Patient;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.PatientRepository;
import com.medbooking.service.AppointmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;
    private final AppointmentService appointmentService;

    public PatientController(PatientRepository patientRepository, AppointmentService appointmentService) {
        this.patientRepository = patientRepository;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/{id}")
    public ApiResponse<Patient> getPatientById(@PathVariable Integer id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy bệnh nhân"));
        return ApiResponse.success(patient);
    }

    @GetMapping("/{id}/appointments")
    public ApiResponse<List<AppointmentResponse>> getAppointments(@PathVariable Integer id) {
        List<AppointmentResponse> history = appointmentService.getAppointmentsByPatientId(id);
        return ApiResponse.success("Lấy lịch sử khám thành công", history);
    }
}
