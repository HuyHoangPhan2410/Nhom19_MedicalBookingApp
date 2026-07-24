package com.medbooking.service.impl;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service quản lý thanh toán và hóa đơn khám bệnh.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, AppointmentRepository appointmentRepository) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Xác nhận thanh toán qua Stored Procedure sp_ConfirmPayment.
     */
    @Override
    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        // Gọi Stored Procedure dưới MySQL
        Map<String, Object> resultMap = appointmentRepository.spConfirmPayment(
                request.getAppointmentId(),
                request.getPaymentMethod()
        );

        Object isSuccessObj = resultMap.get("p_is_success");
        String message = (String) resultMap.get("p_message");

        boolean isSuccess = Boolean.TRUE.equals(isSuccessObj) || (isSuccessObj instanceof Number && ((Number) isSuccessObj).intValue() == 1);

        if (!isSuccess) {
            throw new BusinessException(400, message != null ? message : "Xác nhận thanh toán thất bại");
        }

        // Truy vấn hóa đơn sau khi thanh toán thành công
        Payment payment = paymentRepository.findByAppointmentId(request.getAppointmentId())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin hóa đơn thanh toán"));

        return mapToResponse(payment);
    }

    /**
     * Lấy hóa đơn thanh toán theo ID lịch hẹn.
     */
    @Override
    public PaymentResponse getPaymentByAppointmentId(Integer appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Hóa đơn thanh toán không tồn tại"));
        return mapToResponse(payment);
    }

    /**
     * Chuyển đổi Payment Entity sang DTO PaymentResponse.
     */
    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse res = new PaymentResponse();
        res.setId(payment.getId());
        res.setAppointmentId(payment.getAppointment().getId());
        res.setAmount(payment.getAmount());
        res.setPaymentMethod(payment.getPaymentMethod().name());
        res.setStatus(payment.getStatus().name());
        res.setPaidAt(payment.getPaidAt());
        return res;
    }
}
