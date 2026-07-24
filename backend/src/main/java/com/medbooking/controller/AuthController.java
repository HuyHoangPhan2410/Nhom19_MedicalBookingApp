package com.medbooking.controller;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register-patient")
    public ApiResponse<AuthResponse> registerPatient(@RequestBody RegisterPatientRequest request) {
        AuthResponse response = authService.registerPatient(request);
        return ApiResponse.success("Đăng ký tài khoản thành công", response);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("Đăng nhập thành công", response);
    }
}
