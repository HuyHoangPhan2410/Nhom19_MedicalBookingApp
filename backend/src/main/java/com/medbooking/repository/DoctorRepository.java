package com.medbooking.repository;

import com.medbooking.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý Doctor Entity và Stored Procedure sp_RegisterDoctor.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Integer>, DoctorRepositoryCustom {
    List<Doctor> findBySpecialtyId(Integer specialtyId);
}
