package com.medbooking.controller;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.service.AppointmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller xử lý các yêu cầu liên quan tới lịch hẹn khám bệnh.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ApiResponse<AppointmentResponse> bookAppointment(@RequestBody BookAppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ApiResponse.success("Đặt lịch khám thành công", response);
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable Integer id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ApiResponse.success(response);
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<List<AppointmentResponse>> getAppointmentsByDoctorId(@PathVariable Integer doctorId) {
        List<AppointmentResponse> response = appointmentService.getAppointmentsByDoctorId(doctorId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancelAppointment(@PathVariable Integer id) {
        AppointmentResponse response = appointmentService.cancelAppointment(id);
        return ApiResponse.success("Đã hủy lịch khám thành công", response);
    }

    @PutMapping("/{id}/reschedule")
    public ApiResponse<AppointmentResponse> rescheduleAppointment(
            @PathVariable Integer id,
            @RequestParam Integer newScheduleId) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, newScheduleId);
        return ApiResponse.success("Đổi lịch khám thành công", response);
    }
}
