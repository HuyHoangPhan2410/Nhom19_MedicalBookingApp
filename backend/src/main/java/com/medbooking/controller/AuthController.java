package com.medbooking.controller;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.request.ResendOtpRequest;
import com.medbooking.dto.request.VerifyEmailRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Registration, email verification and login")
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
        return ApiResponse.success("\u0110\u0103ng k\u00fd t\u00e0i kho\u1ea3n th\u00e0nh c\u00f4ng. Vui l\u00f2ng ki\u1ec3m tra email \u0111\u1ec3 nh\u1eadp OTP.", response);
    }

    @PostMapping("/verify-email")
    public ApiResponse<AuthResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request);
        return ApiResponse.success("X\u00e1c minh email th\u00e0nh c\u00f4ng", response);
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ApiResponse.success("G\u1eedi l\u1ea1i OTP th\u00e0nh c\u00f4ng", null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng", response);
    }
}
