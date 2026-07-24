package com.medbooking.controller;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(request);
        return ApiResponse.success("Xác nhận thanh toán thành công", response);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<PaymentResponse> getPaymentByAppointmentId(@PathVariable Integer appointmentId) {
        PaymentResponse response = paymentService.getPaymentByAppointmentId(appointmentId);
        return ApiResponse.success(response);
    }
}
