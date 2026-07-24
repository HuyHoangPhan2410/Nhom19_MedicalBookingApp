package com.medbooking.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Thực thi Stored Procedure sp_CancelSchedule bằng EntityManager.
 */
@Repository
public class ScheduleRepositoryCustomImpl implements ScheduleRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Object> spCancelSchedule(Integer scheduleId, Integer doctorId) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_CancelSchedule");
        
        query.registerStoredProcedureParameter("p_schedule_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_doctor_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_is_success", Boolean.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        query.setParameter("p_schedule_id", scheduleId);
        query.setParameter("p_doctor_id", doctorId);

        query.execute();

        Map<String, Object> result = new HashMap<>();
        result.put("p_is_success", query.getOutputParameterValue("p_is_success"));
        result.put("p_message", query.getOutputParameterValue("p_message"));
        return result;
    }
}
