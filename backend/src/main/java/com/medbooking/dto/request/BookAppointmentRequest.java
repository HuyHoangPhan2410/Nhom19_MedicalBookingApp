package com.medbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public class BookAppointmentRequest {
    @Schema(description = "Patient ID", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer patientId;
    @Schema(description = "Doctor ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer doctorId;
    @Schema(description = "Schedule ID", example = "21", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer scheduleId;
    @Schema(description = "Patient symptoms", example = "Persistent headache")
    private String symptoms;

    public BookAppointmentRequest() {}

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }
}
