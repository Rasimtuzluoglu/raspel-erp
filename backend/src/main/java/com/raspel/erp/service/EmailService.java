package com.raspel.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@raspel-erp.com}")
    private String fromEmail;

    @Async
    public void emailGonder(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("E-posta başarıyla gönderildi -> To: {}, Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("E-posta gönderilirken hata oluştu -> To: {}, Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void faturaBildirimiGonder(String to, String faturaNo, String tutar) {
        String subject = "RasPel ERP - Yeni Fatura Bilgilendirmesi #" + faturaNo;
        String body = String.format("Sayın Müşterimiz,\n\n%s numaralı faturanız oluşturulmuştur. Toplam Tutar: %s TL.\n\nDetaylar için RasPel ERP sistemine giriş yapabilirsiniz.\n\nSaygılarımızla,\nRasPel ERP Ekibi", faturaNo, tutar);
        emailGonder(to, subject, body);
    }
}
