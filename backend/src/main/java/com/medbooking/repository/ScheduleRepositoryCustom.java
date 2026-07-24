package com.medbooking.repository;

import java.util.Map;

public interface ScheduleRepositoryCustom {
    Map<String, Object> spCancelSchedule(Integer scheduleId, Integer doctorId);
}
