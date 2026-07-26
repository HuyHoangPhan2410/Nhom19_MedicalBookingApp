package com.medbooking.service;

import com.medbooking.event.AppointmentBookedEvent;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private AppointmentEmailService appointmentEmailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        appointmentEmailService = new AppointmentEmailService(
                mailSender,
                "sender@gmail.com",
                "sender@gmail.com"
        );
    }

    @Test
    @DisplayName("Email x\u00e1c nh\u1eadn ch\u1ee9a \u0111\u1ea7y \u0111\u1ee7 th\u00f4ng tin l\u1ecbch kh\u00e1m v\u00e0 escape n\u1ed9i dung ng\u01b0\u1eddi d\u00f9ng")
    void sendBookingConfirmationBuildsExpectedEmail() throws Exception {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                123,
                "patient@gmail.com",
                "Nguy\u1ec5n V\u0103n A",
                "BS. Tr\u1ea7n V\u0103n B",
                "N\u1ed9i khoa",
                LocalDate.of(2026, 8, 15),
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                "\u0110au \u0111\u1ea7u <script>alert('x')</script>",
                "pending",
                new BigDecimal("500000")
        );

        appointmentEmailService.sendBookingConfirmation(event);

        verify(mailSender).send(mimeMessage);
        assertEquals("Medical Booking - X\u00e1c nh\u1eadn l\u1ecbch kh\u00e1m #123", mimeMessage.getSubject());
        assertEquals("patient@gmail.com", mimeMessage.getAllRecipients()[0].toString());

        String html = extractText(mimeMessage);
        assertTrue(html.contains("15/08/2026"));
        assertTrue(html.contains("08:00 - 09:30"));
        assertTrue(html.contains("500.000 VN\u0110"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
    }

    private String extractText(Part part) throws Exception {
        if (part.isMimeType("text/*")) {
            return String.valueOf(part.getContent());
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                result.append(extractText(multipart.getBodyPart(index)));
            }
            return result.toString();
        }
        return "";
    }
}
