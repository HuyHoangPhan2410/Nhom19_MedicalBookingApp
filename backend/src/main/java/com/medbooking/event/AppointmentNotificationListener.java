package com.medbooking.event;

import com.medbooking.service.AppointmentEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AppointmentNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentNotificationListener.class);

    private final AppointmentEmailService appointmentEmailService;

    public AppointmentNotificationListener(AppointmentEmailService appointmentEmailService) {
        this.appointmentEmailService = appointmentEmailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentBooked(AppointmentBookedEvent event) {
        try {
            appointmentEmailService.sendBookingConfirmation(event);
        } catch (Exception ex) {
            // Notification failures must not turn a committed booking into an API error.
            LOGGER.warn("Kh\u00f4ng th\u1ec3 g\u1eedi email x\u00e1c nh\u1eadn cho l\u1ecbch h\u1eb9n {}: {}", event.appointmentId(), ex.getMessage());
        }
    }
}
