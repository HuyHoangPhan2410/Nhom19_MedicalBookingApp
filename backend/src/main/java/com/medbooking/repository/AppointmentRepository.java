package com.medbooking.repository;

import com.medbooking.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý entity Appointment và các Stored Procedure liên quan.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer>, AppointmentRepositoryCustom {
    List<Appointment> findByPatientUserId(Integer patientId);
    List<Appointment> findByDoctorUserId(Integer doctorId);
}
