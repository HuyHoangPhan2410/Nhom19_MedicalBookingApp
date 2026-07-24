package com.medbooking.service;

import com.medbooking.dto.request.CreateScheduleRequest;
import com.medbooking.dto.request.UpdateScheduleRequest;
import com.medbooking.dto.response.ScheduleResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Schedule;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.ScheduleRepository;
import com.medbooking.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho các dịch vụ ca làm việc của bác sĩ.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    @DisplayName("Kiểm thử tra cứu danh sách ca khám bác sĩ và kiểm tra trạng thái khả dụng")
    void testGetSchedulesByDoctor() {
        Schedule schedule = new Schedule();
        schedule.setId(1);
        
        Doctor doctor = new Doctor();
        doctor.setUserId(2);
        doctor.setFullName("Dr. John");
        schedule.setDoctor(doctor);
        schedule.setWorkDate(LocalDate.now());
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setMaxPatients(5);

        when(scheduleRepository.findByDoctorUserId(2)).thenReturn(List.of(schedule));
        when(appointmentRepository.findByDoctorUserId(2)).thenReturn(Collections.emptyList());

        List<ScheduleResponse> responses = scheduleService.getSchedulesByDoctor(2, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(5, responses.get(0).getMaxPatients());
        assertEquals(0, responses.get(0).getBookedPatients());
        assertTrue(responses.get(0).getIsAvailable());
    }

    @Test
    @DisplayName("Kiểm thử tạo ca khám mới thành công cho bác sĩ")
    void testCreateScheduleSuccess() {
        Doctor doctor = new Doctor();
        doctor.setUserId(1);
        doctor.setFullName("BS. Nguyễn Văn Hùng");

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDoctorId(1);
        request.setWorkDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(14, 0));
        request.setEndTime(LocalTime.of(15, 30));
        request.setMaxPatients(5);

        when(doctorRepository.findById(1)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.findByDoctorUserIdAndWorkDate(1, request.getWorkDate())).thenReturn(Collections.emptyList());

        Schedule saved = new Schedule();
        saved.setId(100);
        saved.setDoctor(doctor);
        saved.setWorkDate(request.getWorkDate());
        saved.setStartTime(request.getStartTime());
        saved.setEndTime(request.getEndTime());
        saved.setMaxPatients(5);

        when(scheduleRepository.save(any(Schedule.class))).thenReturn(saved);

        ScheduleResponse response = scheduleService.createSchedule(request);

        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals("BS. Nguyễn Văn Hùng", response.getDoctorName());
    }

    @Test
    @DisplayName("Kiểm thử cập nhật ca khám thành công cho bác sĩ")
    void testUpdateScheduleSuccess() {
        Doctor doctor = new Doctor();
        doctor.setUserId(1);
        doctor.setFullName("BS. Nguyễn Văn Hùng");

        Schedule schedule = new Schedule();
        schedule.setId(10);
        schedule.setDoctor(doctor);
        schedule.setWorkDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(9, 30));
        schedule.setMaxPatients(5);

        UpdateScheduleRequest req = new UpdateScheduleRequest();
        req.setDoctorId(1);
        req.setMaxPatients(8);

        when(scheduleRepository.findById(10)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.findByDoctorUserIdAndWorkDate(1, schedule.getWorkDate())).thenReturn(List.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);
        when(appointmentRepository.findByDoctorUserId(1)).thenReturn(Collections.emptyList());

        ScheduleResponse res = scheduleService.updateSchedule(10, req);

        assertNotNull(res);
        assertEquals(8, res.getMaxPatients());
    }

    @Test
    @DisplayName("Kiểm thử hủy ca làm việc của bác sĩ")
    void testCancelScheduleSuccess() {
        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_is_success", 1);
        procResult.put("p_message", "Hủy ca làm việc thành công");

        when(scheduleRepository.spCancelSchedule(10, 1)).thenReturn(procResult);

        assertDoesNotThrow(() -> scheduleService.cancelSchedule(10, 1));
    }
}
