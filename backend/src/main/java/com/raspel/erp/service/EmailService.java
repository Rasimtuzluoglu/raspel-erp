package com.raspel.erp.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@raspel-erp.com}")
    private String fromEmail;

    private static final String TEMPLATE_CSS = """
        <style>
          body { font-family: 'Segoe UI', Arial, sans-serif; background: #f1f5f9; margin: 0; padding: 0; }
          .kapsayici { max-width: 600px; margin: 0 auto; padding: 24px; }
          .ust { background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: #fff; padding: 28px; border-radius: 12px 12px 0 0; text-align: center; }
          .ust h1 { margin: 0; font-size: 20px; }
          .icerik { background: #ffffff; padding: 28px; border-radius: 0 0 12px 12px; }
          .icerik p { color: #334155; font-size: 14px; line-height: 1.6; }
          .kutu { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 16px 0; }
          .kutu table { width: 100%; font-size: 13px; }
          .kutu td { padding: 6px 8px; color: #475569; }
          .kutu td:last-child { text-align: right; font-weight: 600; color: #0f172a; }
          .buton { display: inline-block; background: #3b82f6; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 8px; }
          .alt { text-align: center; padding: 16px; color: #94a3b8; font-size: 12px; }
          .basarili { color: #16a34a; font-weight: 700; }
        </style>
        """;

    @Async
    public void emailGonder(String to, String subject, String body) {
        try {
            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(standartSablon(body), true);
                mailSender.send(message);
                log.info("E-posta başarıyla gönderildi -> To: {}, Subject: {}", to, subject);
            } else {
                log.info("[E-POSTA MOCK] MailSender yapılandırılmadığı için e-posta loga yazıldı -> To: {}, Subject: {}\nBody: {}", to, subject, body);
            }
        } catch (Exception e) {
            log.error("E-posta gönderilirken hata oluştu -> To: {}, Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void faturaBildirimiGonder(String to, String faturaNo, String tutar) {
        String subject = "RasPel ERP - Yeni Fatura Bilgilendirmesi #" + faturaNo;
        String html = sablonUst("Yeni Fatura Oluşturuldu") + """
            <p>Sayın Müşterimiz,</p>
            <p>Hesabınıza <strong>%s</strong> numaralı yeni bir fatura oluşturulmuştur.</p>
            <div class="kutu"><table>
              <tr><td>Fatura No</td><td>#%s</td></tr>
              <tr><td>Toplam Tutar</td><td>%s TL</td></tr>
            </table></div>
            <p>Detaylı görüntüleme için sisteme giriş yapabilirsiniz.</p>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(faturaNo, faturaNo, tutar) + sablonAlt();
        htmlGonder(to, subject, html);
    }

    @Async
    public void siparisBildirimiGonder(String to, String siparisNo, String durum) {
        String subject = "RasPel ERP - Sipariş Durum Güncellemesi #" + siparisNo;
        String html = sablonUst("Sipariş Bilgilendirmesi") + """
            <p>Sayın Müşterimiz,</p>
            <p><strong>%s</strong> numaralı siparişinizin durumu güncellenmiştir.</p>
            <div class="kutu"><table>
              <tr><td>Sipariş No</td><td>#%s</td></tr>
              <tr><td>Yeni Durum</td><td class="basarili">%s</td></tr>
            </table></div>
            <p>Siparişinizin detaylarını sistem üzerinden takip edebilirsiniz.</p>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(siparisNo, siparisNo, durum) + sablonAlt();
        htmlGonder(to, subject, html);
    }

    @Async
    public void sifreSifirlamaGonder(String to, String geciciSifre) {
        String subject = "RasPel ERP - Şifre Sıfırlama";
        String html = sablonUst("Şifre Sıfırlama") + """
            <p>Sayın Kullanıcı,</p>
            <p>Hesabınız için geçici şifreniz oluşturulmuştur:</p>
            <div class="kutu"><table>
              <tr><td>Geçici Şifre</td><td style="font-size:18px;letter-spacing:2px;color:#1d4ed8">%s</td></tr>
            </table></div>
            <p>Güvenliğiniz için giriş yaptıktan sonra şifrenizi değiştirmeyi unutmayın.</p>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(geciciSifre) + sablonAlt();
        htmlGonder(to, subject, html);
    }

    @Async
    public void faturaPdfGonder(String to, byte[] pdfBytes, String faturaNo, String tutar, String aliciAdi) {
        String subject = "Fatura #" + faturaNo + " — RasPel ERP";
        String html = sablonUst("Fatura Ekinde") + """
            <p>Sayın %s,</p>
            <p>Hesabınıza kesilen <strong>#%s</strong> numaralı fatura ektedir.</p>
            <div class="kutu"><table>
              <tr><td>Fatura No</td><td>#%s</td></tr>
              <tr><td>Toplam Tutar</td><td>%s TL</td></tr>
            </table></div>
            <p>Ödeme için faturanın üzerindeki vade tarihine dikkat ediniz.</p>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(aliciAdi != null ? aliciAdi : "Müşterimiz", faturaNo, faturaNo, tutar) + sablonAlt();
        try {
            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                helper.addAttachment("fatura-" + faturaNo + ".pdf", new org.springframework.core.io.ByteArrayResource(pdfBytes));
                mailSender.send(message);
                log.info("Fatura PDF e-postası gönderildi -> To: {}, Fatura: {}", to, faturaNo);
            } else {
                log.info("[E-POSTA MOCK] Fatura PDF e-postası loga yazıldı -> To: {}, Fatura: {}", to, faturaNo);
            }
        } catch (Exception e) {
            log.error("Fatura PDF e-postası gönderilirken hata -> To: {}, Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void odemeHatimlaticiGonder(String to, String faturaNo, String tutar, String kalanTutar, String vade, String cariAdi) {
        String subject = "Hatırlatma: Fatura #" + faturaNo + " ödemesi bekleniyor";
        String html = sablonUst("Ödeme Hatırlatması") + """
            <p>Sayın %s,</p>
            <p><strong>#%s</strong> numaralı faturanız için ödeme hatırlatması yapmak isteriz.</p>
            <div class="kutu"><table>
              <tr><td>Fatura No</td><td>#%s</td></tr>
              <tr><td>Toplam Tutar</td><td>%s TL</td></tr>
              <tr><td>Kalan Tutar</td><td style="color:#dc2626">%s TL</td></tr>
              <tr><td>Vade Tarihi</td><td>%s</td></tr>
            </table></div>
            <p>Faturanın ödenmesini rica ederiz. Zaten ödeme yaptıysanız bu e-postayı dikkate almayınız.</p>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(cariAdi != null ? cariAdi : "Müşterimiz", faturaNo, faturaNo, tutar, kalanTutar, vade) + sablonAlt();
        htmlGonder(to, subject, html);
    }

    @Async
    public void stokUyarisiGonder(String to, String stokAd, String miktar, String birim) {
        String subject = "RasPel ERP - Kritik Stok Uyarısı: " + stokAd;
        String html = sablonUst("Kritik Stok Uyarısı") + """
            <p>Sayın Yönetici,</p>
            <p>Aşağıdaki ürünün stok seviyesi kritik eşiğin altına düşmüştür:</p>
            <div class="kutu"><table>
              <tr><td>Ürün</td><td>%s</td></tr>
              <tr><td>Mevcut Stok</td><td style="color:#dc2626">%s %s</td></tr>
            </table></div>
            <p>Tedarik siparişi oluşturmayı değerlendirebilirsiniz.</p>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(stokAd, miktar, birim) + sablonAlt();
        htmlGonder(to, subject, html);
    }

    @Async
    public void hoşGeldinGonder(String to, String kullaniciAdi) {
        String subject = "RasPel ERP'ye Hoş Geldiniz!";
        String html = sablonUst("Hoş Geldiniz " + kullaniciAdi + "!") + """
            <p>RasPel ERP sistemine başarıyla kaydoldunuz.</p>
            <p>Artık tüm iş süreçlerinizi tek platform üzerinden yönetebilirsiniz:</p>
            <div class="kutu" style="font-size:13px;color:#475569">
              • Cari hesap ve fatura yönetimi<br/>
              • Stok ve depo takibi<br/>
              • Satış ve satın alma süreçleri<br/>
              • Personel ve İK yönetimi<br/>
              • Detaylı raporlama
            </div>
            <p>Saygılarımızla,<br/><strong>RasPel ERP Ekibi</strong></p>
            """.formatted(kullaniciAdi) + sablonAlt();
        htmlGonder(to, subject, html);
    }

    private void htmlGonder(String to, String subject, String html) {
        try {
            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(message);
                log.info("HTML e-posta gönderildi -> To: {}, Subject: {}", to, subject);
            } else {
                log.info("[E-POSTA MOCK] HTML e-posta loga yazıldı -> To: {}, Subject: {}", to, subject);
            }
        } catch (Exception e) {
            log.error("HTML e-posta gönderilirken hata -> To: {}, Error: {}", to, e.getMessage());
        }
    }

    private String sablonUst(String baslik) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" + TEMPLATE_CSS + "</head><body>" +
               "<div class='kapsayici'><div class='ust'><h1>" + baslik + "</h1></div><div class='icerik'>";
    }

    private String sablonAlt() {
        return "</div><div class='alt'>Bu e-posta RasPel ERP sistemi tarafından otomatik gönderilmiştir. Lütfen yanıtlamayınız.</div></div></body></html>";
    }

    private String standartSablon(String metin) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" + TEMPLATE_CSS + "</head><body>" +
               "<div class='kapsayici'><div class='icerik'><p>" + metin.replace("\n", "<br/>") + "</p></div>" +
               "<div class='alt'>RasPel ERP</div></div></body></html>";
    }
}
