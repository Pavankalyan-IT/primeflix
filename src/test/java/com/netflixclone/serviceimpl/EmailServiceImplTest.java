package com.netflixclone.serviceimpl;

import com.netflixclone.exception.EmailSendingException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;

class EmailServiceImplTest {

    @Test
    void explainsSmtpAuthenticationFailureWithoutExposingCredentials() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailAuthenticationException("credentials rejected"))
                .when(sender).send(org.mockito.ArgumentMatchers.any(
                        org.springframework.mail.SimpleMailMessage.class));
        EmailServiceImpl service = new EmailServiceImpl(sender, "sender@example.com", "http://localhost:8088");

        EmailSendingException exception = assertThrows(EmailSendingException.class,
                () -> service.sendVerificationEmail("recipient@example.com", "one-time-token"));

        assertTrue(exception.getMessage().contains("SMTP authentication failed"));
        assertTrue(exception.getMessage().contains("App Password"));
    }
}
