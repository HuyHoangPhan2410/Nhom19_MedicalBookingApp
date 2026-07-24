package com.medbooking.repository;

import java.util.Map;

public interface AppointmentRepositoryCustom {
    Map<String, Object> spBookAppointment(Integer patientId, Integer doctorId, Integer scheduleId, String symptoms);
    Map<String, Object> spConfirmPayment(Integer appointmentId, String paymentMethod);
    Map<String, Object> spCancelAppointment(Integer appointmentId, String cancelledByRole);
    Map<String, Object> spRescheduleAppointment(Integer appointmentId, Integer newScheduleId);
}
