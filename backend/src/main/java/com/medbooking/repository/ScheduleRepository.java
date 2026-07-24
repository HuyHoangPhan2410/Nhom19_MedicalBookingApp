package com.medbooking.repository;

import com.medbooking.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository quản lý Schedule Entity và Stored Procedure sp_CancelSchedule.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer>, ScheduleRepositoryCustom {
    List<Schedule> findByDoctorUserId(Integer doctorId);
    List<Schedule> findByDoctorUserIdAndWorkDate(Integer doctorId, LocalDate workDate);
}
