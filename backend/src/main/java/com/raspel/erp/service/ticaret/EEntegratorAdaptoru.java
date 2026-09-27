package com.raspel.erp.service.ticaret;

/**
 * GİB/entegratör adaptör sözleşmesi. Canlı entegrasyon müşterinin entegratör bilgileriyle
 * (app.entegrator.url, app.entegrator.api-key) devreye alınır; varsayılan uygulama
 * {@link HttpEEntegratorAdaptoru}'dur. Farklı bir entegratör için bu arayüz uygulanır.
 */
public interface EEntegratorAdaptoru {

    /** Entegratör yapılandırılmış mı? Değilse gönderim/sorgulama yapılamaz. */
    boolean tanimliMi();

    /** UBL-TR XML belgesini entegratöre iletir; başarılıysa true döner. */
    boolean gonder(String ettn, String belgeTuru, String ublXml);

    /** Belgenin GİB durum kodunu sorgular; alınamazsa null döner. */
    Integer durumSorgula(String ettn);
}
