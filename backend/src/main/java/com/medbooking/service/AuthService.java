package com.medbooking.service;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.request.RegisterPatientRequest;
import com.medbooking.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse registerPatient(RegisterPatientRequest request);
    AuthResponse login(LoginRequest request);
}
