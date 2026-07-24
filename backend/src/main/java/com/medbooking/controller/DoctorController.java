package com.medbooking.controller;

import com.medbooking.dto.request.RegisterDoctorRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.DoctorResponse;
import com.medbooking.service.DoctorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ApiResponse<List<DoctorResponse>> getAllDoctors() {
        return ApiResponse.success(doctorService.getAllDoctors());
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorResponse> getDoctorById(@PathVariable Integer id) {
        return ApiResponse.success(doctorService.getDoctorById(id));
    }

    @GetMapping("/specialty/{specialtyId}")
    public ApiResponse<List<DoctorResponse>> getDoctorsBySpecialty(@PathVariable Integer specialtyId) {
        return ApiResponse.success(doctorService.getDoctorsBySpecialty(specialtyId));
    }

    @PostMapping("/register")
    public ApiResponse<DoctorResponse> registerDoctor(@RequestBody RegisterDoctorRequest request) {
        DoctorResponse response = doctorService.registerDoctor(request);
        return ApiResponse.success("Đăng ký bác sĩ thành công", response);
    }
}
