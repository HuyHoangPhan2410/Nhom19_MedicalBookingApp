package com.medbooking.service;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.AppointmentResponse;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse bookAppointment(BookAppointmentRequest request);
    AppointmentResponse getAppointmentById(Integer id);
    List<AppointmentResponse> getAppointmentsByPatientId(Integer patientId);
    List<AppointmentResponse> getAppointmentsByDoctorId(Integer doctorId);
    AppointmentResponse cancelAppointment(Integer id);
    AppointmentResponse rescheduleAppointment(Integer id, Integer newScheduleId);
}
