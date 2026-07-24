package com.medbooking.service;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse confirmPayment(ConfirmPaymentRequest request);
    PaymentResponse getPaymentByAppointmentId(Integer appointmentId);
}
