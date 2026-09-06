package com.raspel.erp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * E-posta yapılandırması.
 * SMTP kullanıcı adı tanımlıysa gerçek JavaMailSender oluşturulur;
 * aksi halde Spring'in varsayılan (auth'sız) mail sender'ı devreye girmesin diye
 * yalnızca tanımlıysa bean üretilir. Böylece SMTP yapılandırılmadığında
 * EmailService.mock akışına düşer ve "Authentication failed" log hataları oluşmaz.
 */
@Configuration
public class MailConfig {

    @ConditionalOnExpression("'${spring.mail.username:}' != ''")
    @Bean
    public JavaMailSender javaMailSender(
            org.springframework.core.env.Environment env) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(env.getProperty("spring.mail.host", "smtp.gmail.com"));
        sender.setPort(Integer.parseInt(env.getProperty("spring.mail.port", "587")));
        sender.setUsername(env.getProperty("spring.mail.username"));
        sender.setPassword(env.getProperty("spring.mail.password"));
        sender.setDefaultEncoding("UTF-8");
        java.util.Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
