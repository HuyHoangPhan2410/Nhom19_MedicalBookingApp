package com.medbooking.service;

import com.medbooking.dto.request.RegisterDoctorRequest;
import com.medbooking.dto.response.DoctorResponse;

import java.util.List;

public interface DoctorService {
    List<DoctorResponse> getAllDoctors();
    DoctorResponse getDoctorById(Integer id);
    List<DoctorResponse> getDoctorsBySpecialty(Integer specialtyId);
    DoctorResponse registerDoctor(RegisterDoctorRequest request);
}
