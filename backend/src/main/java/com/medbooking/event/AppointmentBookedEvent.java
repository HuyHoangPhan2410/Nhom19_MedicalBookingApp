package com.medbooking.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentBookedEvent(
        Integer appointmentId,
        String recipientEmail,
        String patientName,
        String doctorName,
        String specialtyName,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime,
        String symptoms,
        String status,
        BigDecimal consultationFee
) {
}