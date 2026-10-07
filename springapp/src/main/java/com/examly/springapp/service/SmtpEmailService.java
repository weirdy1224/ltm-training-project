package com.examly.springapp.service;

import com.examly.springapp.exception.ApiCommunicationException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * SMTP implementation of {@link EmailService}: every mail has a plain-text and an HTML part.
 * Codes are never written to the logs, except when app.otp.dev-log=true AND no SMTP login is configured
 * (a local-development switch so the flow can be tried without a mail account).
 *
 * @author Lokesh
 */
@Service
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);
    private static final String BRAND = "Online E-Learning Platform";

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean devLog;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${spring.mail.username:}") String from,
                            @Value("${app.otp.dev-log:false}") boolean devLog) {
        this.mailSender = mailSender;
        this.from = from == null ? "" : from.trim();
        this.devLog = devLog;
    }

    @Override
    public void sendOtpEmail(String to, String name, String code, int validMinutes) {
        log.trace("sendOtpEmail entered");
        if (from.isEmpty()) {
            if (devLog) {
                log.warn("DEV ONLY - SMTP is not configured, the OTP for {} is {}", to, code);
                return;
            }
            throw new ApiCommunicationException("SMTP is not configured: set MAIL_USERNAME and MAIL_PASSWORD",
                    new IllegalStateException("spring.mail.username is empty"));
        }
        String safeName = HtmlUtils.htmlEscape(name == null || name.isBlank() ? "there" : name);
        String text = "Hi " + (name == null || name.isBlank() ? "there" : name) + ",\n\n"
                + "Your verification code is: " + code + "\n\n"
                + "It is valid for " + validMinutes + " minutes. If you did not create an account, please ignore this email.\n\n"
                + BRAND;
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;border:1px solid #e5e7eb;border-radius:12px\">"
                + "<h2 style=\"margin:0 0 12px\">" + BRAND + "</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Use this code to verify your email address:</p>"
                + "<p style=\"font-size:32px;letter-spacing:8px;font-weight:bold;margin:16px 0\">" + code + "</p>"
                + "<p style=\"color:#6b7280\">The code is valid for " + validMinutes
                + " minutes. If you did not create an account, you can ignore this email.</p></div>";
        send(to, "Your verification code: " + code, text, html);
        log.info("OTP email sent");
    }

    @Override
    public void sendInstructorApprovedEmail(String to, String name) {
        log.trace("sendInstructorApprovedEmail entered");
        if (from.isEmpty()) {
            log.debug("SMTP not configured: approval notice not sent");
            return;
        }
        String safeName = HtmlUtils.htmlEscape(name == null || name.isBlank() ? "there" : name);
        String text = "Hi " + (name == null || name.isBlank() ? "there" : name) + ",\n\n"
                + "Your instructor account has been approved. You can log in now and start adding courses and video lessons.\n\n"
                + BRAND;
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px\">"
                + "<h2>" + BRAND + "</h2><p>Hi " + safeName + ",</p>"
                + "<p>Your instructor account has been <b>approved</b>. You can log in now and start adding courses and video lessons.</p></div>";
        try {
            send(to, "Your instructor account is approved", text, html);
        } catch (ApiCommunicationException e) {
            log.warn("Could not send the approval notice: {}", e.getMessage());
        }
    }

    private void send(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException e) {
            // technical details stay in the log; the client only sees "upstream service unavailable"
            throw new ApiCommunicationException("Could not send an email over SMTP", e);
        }
    }
}
