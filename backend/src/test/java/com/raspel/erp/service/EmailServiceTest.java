package com.raspel.erp.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.EmailService;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailService emailService;

    private void hazirla() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@raspel-erp.com");
    }

    @Test
    void emailGonder_mailSenderYoksaCokmez() {
        // mailSender null ise mock moda düşer ve hata fırlatmaz
        ReflectionTestUtils.setField(emailService, "mailSender", null);
        assertDoesNotThrow(() -> emailService.emailGonder("test@test.com", "Konu", "İçerik"));
    }

    @Test
    void emailGonder_mailGonderir() throws Exception {
        hazirla();
        MimeMessage mesaj = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mesaj);

        emailService.emailGonder("test@test.com", "Test Konu", "Test İçerik");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void faturaBildirimiGonder_htmlSablonIcerir() throws Exception {
        hazirla();
        MimeMessage mesaj = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mesaj);

        emailService.faturaBildirimiGonder("musteri@test.com", "FTR-2026-001", "1250.00");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void siparisBildirimiGonder_cagirilir() throws Exception {
        hazirla();
        MimeMessage mesaj = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mesaj);

        emailService.siparisBildirimiGonder("test@test.com", "SIP-100", "TAMAMLANDI");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sifreSifirlamaGonder_cagirilir() throws Exception {
        hazirla();
        MimeMessage mesaj = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mesaj);

        emailService.sifreSifirlamaGonder("test@test.com", "Gecici123");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void stokUyarisiGonder_cagirilir() throws Exception {
        hazirla();
        MimeMessage mesaj = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mesaj);

        emailService.stokUyarisiGonder("yonetici@test.com", "MDF 18mm", "3", "Adet");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void mailGonderimHatasıYutulur() throws Exception {
        hazirla();
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP bağlantı hatası"));

        assertDoesNotThrow(() -> emailService.emailGonder("test@test.com", "Konu", "İçerik"));
    }
}