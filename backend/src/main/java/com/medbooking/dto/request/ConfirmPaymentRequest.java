package com.medbooking.dto.request;

public class ConfirmPaymentRequest {
    private Integer appointmentId;
    private String paymentMethod;

    public ConfirmPaymentRequest() {}

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
