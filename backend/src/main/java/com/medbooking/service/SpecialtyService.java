package com.medbooking.service;

import com.medbooking.dto.response.SpecialtyResponse;

import java.util.List;

public interface SpecialtyService {
    List<SpecialtyResponse> getAllSpecialties();
    SpecialtyResponse getSpecialtyById(Integer id);
}
