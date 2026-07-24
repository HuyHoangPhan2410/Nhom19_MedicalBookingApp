package com.medbooking.service;

import com.medbooking.dto.request.CreateScheduleRequest;
import com.medbooking.dto.request.UpdateScheduleRequest;
import com.medbooking.dto.response.ScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    List<ScheduleResponse> getSchedulesByDoctor(Integer doctorId, LocalDate workDate);
    ScheduleResponse createSchedule(CreateScheduleRequest request);
    ScheduleResponse updateSchedule(Integer scheduleId, UpdateScheduleRequest request);
    void cancelSchedule(Integer scheduleId, Integer doctorId);
}
