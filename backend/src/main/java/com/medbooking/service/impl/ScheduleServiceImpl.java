package com.medbooking.service.impl;

import com.medbooking.dto.request.CreateScheduleRequest;
import com.medbooking.dto.request.UpdateScheduleRequest;
import com.medbooking.dto.response.ScheduleResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Schedule;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.ScheduleRepository;
import com.medbooking.service.ScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service quản lý ca làm việc của bác sĩ (Hỗ trợ đầy đủ CRUD: Create, Read, Update, Delete/Cancel).
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public ScheduleServiceImpl(
            ScheduleRepository scheduleRepository,
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * [READ] Lấy danh sách ca làm việc của bác sĩ theo ngày và tính số slot còn trống.
     */
    @Override
    public List<ScheduleResponse> getSchedulesByDoctor(Integer doctorId, LocalDate workDate) {
        List<Schedule> schedules;
        if (workDate != null) {
            schedules = scheduleRepository.findByDoctorUserIdAndWorkDate(doctorId, workDate);
        } else {
            schedules = scheduleRepository.findByDoctorUserId(doctorId);
        }

        return schedules.stream().map(s -> {
            ScheduleResponse res = new ScheduleResponse();
            res.setId(s.getId());
            res.setDoctorId(s.getDoctor().getUserId());
            res.setDoctorName(s.getDoctor().getFullName());
            res.setWorkDate(s.getWorkDate());
            res.setStartTime(s.getStartTime());
            res.setEndTime(s.getEndTime());
            res.setMaxPatients(s.getMaxPatients());
            
            // Tính số lượng bệnh nhân đã đặt lịch trong ca này
            int booked = (int) appointmentRepository.findByDoctorUserId(doctorId).stream()
                    .filter(a -> a.getSchedule().getId().equals(s.getId()))
                    .filter(a -> a.getStatus() == com.medbooking.entity.Appointment.Status.pending 
                              || a.getStatus() == com.medbooking.entity.Appointment.Status.confirmed)
                    .count();
            
            res.setBookedPatients(booked);
            res.setIsAvailable(booked < s.getMaxPatients());
            return res;
        }).collect(Collectors.toList());
    }

    /**
     * [CREATE] Đăng ký tạo ca làm việc mới cho bác sĩ.
     */
    @Override
    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy hồ sơ bác sĩ"));

        if (request.getWorkDate() == null || request.getWorkDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "Ngày đăng ký ca khám không được thuộc về quá khứ");
        }

        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(400, "Giờ kết thúc phải lớn hơn giờ bắt đầu ca khám");
        }

        // Kiểm tra ca khám trùng lặp
        List<Schedule> existing = scheduleRepository.findByDoctorUserIdAndWorkDate(request.getDoctorId(), request.getWorkDate());
        boolean isOverlapping = existing.stream().anyMatch(s -> 
            request.getStartTime().isBefore(s.getEndTime()) && request.getEndTime().isAfter(s.getStartTime())
        );

        if (isOverlapping) {
            throw new BusinessException(400, "Khung giờ này bị trùng lặp với một ca khám đã đăng ký trước đó trong ngày");
        }

        Schedule schedule = new Schedule();
        schedule.setDoctor(doctor);
        schedule.setWorkDate(request.getWorkDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMaxPatients(request.getMaxPatients() != null && request.getMaxPatients() > 0 ? request.getMaxPatients() : 5);

        Schedule saved = scheduleRepository.save(schedule);

        ScheduleResponse res = new ScheduleResponse();
        res.setId(saved.getId());
        res.setDoctorId(doctor.getUserId());
        res.setDoctorName(doctor.getFullName());
        res.setWorkDate(saved.getWorkDate());
        res.setStartTime(saved.getStartTime());
        res.setEndTime(saved.getEndTime());
        res.setMaxPatients(saved.getMaxPatients());
        res.setBookedPatients(0);
        res.setIsAvailable(true);
        return res;
    }

    /**
     * [UPDATE] Cập nhật thông tin ca khám của bác sĩ.
     */
    @Override
    @Transactional
    public ScheduleResponse updateSchedule(Integer scheduleId, UpdateScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy ca khám cần cập nhật"));

        if (!schedule.getDoctor().getUserId().equals(request.getDoctorId())) {
            throw new BusinessException(403, "Bạn không có quyền chỉnh sửa ca khám của bác sĩ khác");
        }

        if (request.getWorkDate() != null && request.getWorkDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "Ngày ca khám không được thuộc về quá khứ");
        }

        if (request.getStartTime() != null && request.getEndTime() != null && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(400, "Giờ kết thúc phải lớn hơn giờ bắt đầu ca khám");
        }

        LocalDate targetDate = request.getWorkDate() != null ? request.getWorkDate() : schedule.getWorkDate();
        java.time.LocalTime targetStart = request.getStartTime() != null ? request.getStartTime() : schedule.getStartTime();
        java.time.LocalTime targetEnd = request.getEndTime() != null ? request.getEndTime() : schedule.getEndTime();

        // Kiểm tra ca khám trùng lặp ngoại trừ chính ca đang sửa
        List<Schedule> existing = scheduleRepository.findByDoctorUserIdAndWorkDate(request.getDoctorId(), targetDate);
        boolean isOverlapping = existing.stream()
                .filter(s -> !s.getId().equals(scheduleId))
                .anyMatch(s -> targetStart.isBefore(s.getEndTime()) && targetEnd.isAfter(s.getStartTime()));

        if (isOverlapping) {
            throw new BusinessException(400, "Khung giờ cập nhật bị trùng lặp với ca khám khác trong ngày");
        }

        if (request.getWorkDate() != null) schedule.setWorkDate(request.getWorkDate());
        if (request.getStartTime() != null) schedule.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) schedule.setEndTime(request.getEndTime());
        if (request.getMaxPatients() != null && request.getMaxPatients() > 0) schedule.setMaxPatients(request.getMaxPatients());

        Schedule updated = scheduleRepository.save(schedule);

        int booked = (int) appointmentRepository.findByDoctorUserId(request.getDoctorId()).stream()
                .filter(a -> a.getSchedule().getId().equals(updated.getId()))
                .filter(a -> a.getStatus() == com.medbooking.entity.Appointment.Status.pending 
                          || a.getStatus() == com.medbooking.entity.Appointment.Status.confirmed)
                .count();

        ScheduleResponse res = new ScheduleResponse();
        res.setId(updated.getId());
        res.setDoctorId(updated.getDoctor().getUserId());
        res.setDoctorName(updated.getDoctor().getFullName());
        res.setWorkDate(updated.getWorkDate());
        res.setStartTime(updated.getStartTime());
        res.setEndTime(updated.getEndTime());
        res.setMaxPatients(updated.getMaxPatients());
        res.setBookedPatients(booked);
        res.setIsAvailable(booked < updated.getMaxPatients());
        return res;
    }

    /**
     * [DELETE/CANCEL] Hủy ca làm việc của bác sĩ qua Stored Procedure sp_CancelSchedule.
     */
    @Override
    @Transactional
    public void cancelSchedule(Integer scheduleId, Integer doctorId) {
        Map<String, Object> resultMap = scheduleRepository.spCancelSchedule(scheduleId, doctorId);

        Object isSuccessObj = resultMap.get("p_is_success");
        String message = (String) resultMap.get("p_message");

        boolean isSuccess = Boolean.TRUE.equals(isSuccessObj) || (isSuccessObj instanceof Number && ((Number) isSuccessObj).intValue() == 1);

        if (!isSuccess) {
            throw new BusinessException(400, message != null ? message : "Hủy ca làm việc thất bại");
        }
    }
}
