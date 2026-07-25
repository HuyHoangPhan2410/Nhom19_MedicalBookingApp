package com.medbooking.service.impl;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key:}")
    private String stripePublishableKey;

    public PaymentServiceImpl(PaymentRepository paymentRepository, AppointmentRepository appointmentRepository) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isEmpty()
                && !stripeSecretKey.equals("sk_test_placeholder")) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        Map<String, Object> resultMap = appointmentRepository.spConfirmPayment(
                request.getAppointmentId(),
                request.getPaymentMethod()
        );

        Object isSuccessObj = resultMap.get("p_is_success");
        String message = (String) resultMap.get("p_message");

        boolean isSuccess = Boolean.TRUE.equals(isSuccessObj)
                || (isSuccessObj instanceof Number && ((Number) isSuccessObj).intValue() == 1);

        if (!isSuccess) {
            throw new BusinessException(400, message != null ? message : "Xác nhận thanh toán thất bại");
        }

        Payment payment = paymentRepository.findByAppointmentId(request.getAppointmentId())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin hóa đơn thanh toán"));

        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByAppointmentId(Integer appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Hóa đơn thanh toán không tồn tại"));
        return mapToResponse(payment);
    }

    // ✅ Tạo Stripe PaymentIntent
    @Override
    @Transactional
    public Map<String, Object> createPaymentIntent(Integer appointmentId) {
        if (Stripe.apiKey == null || Stripe.apiKey.isEmpty()) {
            throw new BusinessException(500, "Stripe chưa được cấu hình. Vui lòng set STRIPE_SECRET_KEY.");
        }

        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy hóa đơn thanh toán"));

        if (payment.getStatus() == Payment.Status.success) {
            throw new BusinessException(400, "Đơn hàng này đã được thanh toán");
        }

        try {
            long amountInVnd = payment.getAmount().longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInVnd)
                    .setCurrency("vnd")
                    .setDescription("Thanh toán khám bệnh - Đơn #" + appointmentId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Lưu Stripe PaymentIntent ID vào DB
            payment.setStripePaymentIntentId(intent.getId());
            payment.setPaymentMethod(Payment.PaymentMethod.card);
            paymentRepository.save(payment);

            Map<String, Object> result = new HashMap<>();
            result.put("clientSecret", intent.getClientSecret());
            result.put("paymentIntentId", intent.getId());
            result.put("amount", amountInVnd);
            return result;

        } catch (Exception e) {
            throw new BusinessException(500, "Lỗi tạo PaymentIntent: " + e.getMessage());
        }
    }

    // ✅ Trả về Stripe Publishable Key
    @Override
    public String getStripePublishableKey() {
        return stripePublishableKey;
    }

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