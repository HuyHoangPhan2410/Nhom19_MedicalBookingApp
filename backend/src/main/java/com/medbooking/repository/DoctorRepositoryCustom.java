package com.medbooking.repository;

import java.math.BigDecimal;
import java.util.Map;

public interface DoctorRepositoryCustom {
    Map<String, Object> spRegisterDoctor(
            String email,
            String passwordHash,
            Integer specialtyId,
            String fullName,
            Integer experienceYears,
            BigDecimal consultationFee,
            String bio
    );
}
