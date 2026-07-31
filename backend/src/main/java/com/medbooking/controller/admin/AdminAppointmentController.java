package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Administration", description = "Admin login, dashboard and CRUD APIs")
@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    private final AppointmentRepository appointmentRepository;

    public AdminAppointmentController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping
    public ApiResponse<List<AppointmentResponse>> getAll() {
        List<AppointmentResponse> list = appointmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AppointmentResponse> updateStatus(@PathVariable Integer id,
                                                          @RequestBody Map<String, String> body) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));

        String newStatus = body.get("status");
        app.setStatus(Appointment.Status.valueOf(newStatus));
        appointmentRepository.save(app);

        return ApiResponse.success("Cập nhật trạng thái thành công", mapToResponse(app));
    }

    private AppointmentResponse mapToResponse(Appointment app) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(app.getId());
        res.setPatientId(app.getPatient().getUserId());
        res.setPatientName(app.getPatient().getFullName());
        res.setDoctorId(app.getDoctor().getUserId());
        res.setDoctorName(app.getDoctor().getFullName());
        res.setSpecialtyName(app.getDoctor().getSpecialty().getName());
        res.setScheduleId(app.getSchedule().getId());
        res.setWorkDate(app.getSchedule().getWorkDate());
        res.setStartTime(app.getSchedule().getStartTime());
        res.setEndTime(app.getSchedule().getEndTime());
        res.setStatus(app.getStatus().name());
        res.setSymptoms(app.getSymptoms());
        res.setCreatedAt(app.getCreatedAt());
        return res;
    }
}