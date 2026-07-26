package com.medbooking.service;

import com.medbooking.event.AppointmentBookedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class AppointmentEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String mailFrom;

    public AppointmentEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.from:}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.mailFrom = mailFrom;
    }

    public void sendBookingConfirmation(AppointmentBookedEvent event) {
        if (!StringUtils.hasText(mailUsername)) {
            throw new IllegalStateException("Gmail SMTP ch\u01b0a \u0111\u01b0\u1ee3c c\u1ea5u h\u00ecnh");
        }
        if (!StringUtils.hasText(event.recipientEmail())) {
            throw new IllegalArgumentException("Email b\u1ec7nh nh\u00e2n kh\u00f4ng h\u1ee3p l\u1ec7");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(event.recipientEmail());
            helper.setFrom(StringUtils.hasText(mailFrom) ? mailFrom : mailUsername);
            helper.setSubject("Medical Booking - X\u00e1c nh\u1eadn l\u1ecbch kh\u00e1m #" + event.appointmentId());
            helper.setText(buildEmailHtml(event), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Kh\u00f4ng th\u1ec3 t\u1ea1o email x\u00e1c nh\u1eadn l\u1ecbch kh\u00e1m", ex);
        }
    }

    private String buildEmailHtml(AppointmentBookedEvent event) {
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#0f172a;max-width:620px;margin:auto">
                  <div style="background:#0f766e;color:white;padding:20px 24px;border-radius:12px 12px 0 0">
                    <h2 style="margin:0">Medical Booking</h2>
                    <p style="margin:6px 0 0">X\u00e1c nh\u1eadn \u0111\u1eb7t l\u1ecbch kh\u00e1m th\u00e0nh c\u00f4ng</p>
                  </div>
                  <div style="border:1px solid #dbe4ea;border-top:0;padding:24px;border-radius:0 0 12px 12px">
                    <p>Xin ch\u00e0o <strong>%s</strong>,</p>
                    <p>L\u1ecbch kh\u00e1m c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c ghi nh\u1eadn. Th\u00f4ng tin c\u1ee5 th\u1ec3 nh\u01b0 sau:</p>
                    <table style="width:100%%;border-collapse:collapse;margin:18px 0">
                      %s
                    </table>
                    <div style="background:#f0fdfa;border-left:4px solid #0f766e;padding:12px 16px;margin-top:20px">
                      Vui l\u00f2ng c\u00f3 m\u1eb7t tr\u01b0\u1edbc gi\u1edd h\u1eb9n kho\u1ea3ng 15 ph\u00fat \u0111\u1ec3 ho\u00e0n t\u1ea5t th\u1ee7 t\u1ee5c.
                    </div>
                    <p style="margin-top:22px;color:#475569">N\u1ebfu c\u1ea7n thay \u0111\u1ed5i ho\u1eb7c h\u1ee7y l\u1ecbch, vui l\u00f2ng \u0111\u0103ng nh\u1eadp Medical Booking \u0111\u1ec3 th\u1ef1c hi\u1ec7n.</p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(event.patientName()),
                row("M\u00e3 l\u1ecbch h\u1eb9n", "#" + event.appointmentId())
                        + row("B\u00e1c s\u0129", event.doctorName())
                        + row("Chuy\u00ean khoa", event.specialtyName())
                        + row("Ng\u00e0y kh\u00e1m", event.workDate().format(DATE_FORMAT))
                        + row("Th\u1eddi gian", event.startTime().format(TIME_FORMAT) + " - " + event.endTime().format(TIME_FORMAT))
                        + row("Chi ph\u00ed d\u1ef1 ki\u1ebfn", formatFee(event.consultationFee()))
                        + row("Tri\u1ec7u ch\u1ee9ng", StringUtils.hasText(event.symptoms()) ? event.symptoms() : "Kh\u00f4ng c\u00f3")
                        + row("Tr\u1ea1ng th\u00e1i", translateStatus(event.status()))
        );
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;color:#475569;width:38%%">%s</td>
                  <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;font-weight:600">%s</td>
                </tr>
                """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private String formatFee(BigDecimal fee) {
        if (fee == null) {
            return "Li\u00ean h\u1ec7 ph\u00f2ng kh\u00e1m";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        return formatter.format(fee) + " VN\u0110";
    }

    private String translateStatus(String status) {
        if (status == null) {
            return "Ch\u1edd x\u00e1c nh\u1eadn";
        }
        return switch (status) {
            case "confirmed" -> "\u0110\u00e3 x\u00e1c nh\u1eadn";
            case "cancelled" -> "\u0110\u00e3 h\u1ee7y";
            case "completed" -> "\u0110\u00e3 ho\u00e0n th\u00e0nh";
            default -> "Ch\u1edd x\u00e1c nh\u1eadn";
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
