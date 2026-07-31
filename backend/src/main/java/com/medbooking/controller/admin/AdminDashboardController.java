package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Administration", description = "Admin login, dashboard and CRUD APIs")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardController(UserRepository userRepository,
                                     DoctorRepository doctorRepository,
                                     PatientRepository patientRepository,
                                     AppointmentRepository appointmentRepository,
                                     SpecialtyRepository specialtyRepository,
                                     PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.specialtyRepository = specialtyRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDoctors", doctorRepository.count());
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalSpecialties", specialtyRepository.count());
        stats.put("totalAppointments", appointmentRepository.count());

        long pendingCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == Appointment.Status.pending).count();
        long confirmedCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == Appointment.Status.confirmed).count();
        long cancelledCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == Appointment.Status.cancelled).count();

        stats.put("pendingAppointments", pendingCount);
        stats.put("confirmedAppointments", confirmedCount);
        stats.put("cancelledAppointments", cancelledCount);

        return ApiResponse.success(stats);
    }
}