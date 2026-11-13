package com.netflixclone.serviceimpl;

import com.netflixclone.exception.EmailSendingException;
import com.netflixclone.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String sender;
    private final String frontendUrl;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String sender,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.sender = sender;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        send(toEmail, "Verify your account",
                "Verify your email by opening this link:\n"
                        + frontendUrl + "/verify-email?token=" + encode(token));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        send(toEmail, "Reset your password",
                "Reset your password by opening this link:\n"
                        + frontendUrl + "/reset-password?token=" + encode(token));
    }

    private void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            if (exception instanceof MailAuthenticationException) {
                throw new EmailSendingException(
                        "Email could not be sent because SMTP authentication failed. Check MAIL_USERNAME and MAIL_PASSWORD; for Gmail, use an App Password.",
                        exception);
            }
            throw new EmailSendingException("Email could not be sent", exception);
        }
    }

    private String encode(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
