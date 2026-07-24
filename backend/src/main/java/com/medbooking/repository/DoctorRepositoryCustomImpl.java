package com.medbooking.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Thực thi Stored Procedure sp_RegisterDoctor bằng EntityManager.
 */
@Repository
public class DoctorRepositoryCustomImpl implements DoctorRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Object> spRegisterDoctor(
            String email,
            String passwordHash,
            Integer specialtyId,
            String fullName,
            Integer experienceYears,
            BigDecimal consultationFee,
            String bio
    ) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_RegisterDoctor");
        
        query.registerStoredProcedureParameter("p_email", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_password", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_specialty_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_full_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_experience_years", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_consultation_fee", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_bio", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_doctor_id", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        query.setParameter("p_email", email);
        query.setParameter("p_password", passwordHash);
        query.setParameter("p_specialty_id", specialtyId);
        query.setParameter("p_full_name", fullName);
        query.setParameter("p_experience_years", experienceYears);
        query.setParameter("p_consultation_fee", consultationFee);
        query.setParameter("p_bio", bio);

        query.execute();

        Map<String, Object> result = new HashMap<>();
        result.put("p_doctor_id", query.getOutputParameterValue("p_doctor_id"));
        result.put("p_message", query.getOutputParameterValue("p_message"));
        return result;
    }
}
