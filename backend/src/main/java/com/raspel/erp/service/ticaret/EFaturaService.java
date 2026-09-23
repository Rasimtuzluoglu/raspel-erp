package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.EFaturaDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.entity.ticaret.EFatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.repository.ticaret.EFaturaRepository;
import com.raspel.erp.service.ticaret.FaturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.raspel.erp.entity.ticaret.Fatura;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EFaturaService {

    private final EFaturaRepository eFaturaRepository;
    private final FaturaService faturaService;
    private final CariHesapRepository cariHesapRepository;
    private final SirketRepository sirketRepository;
    private final TenantChecker tenantChecker;
    private final RestTemplate restTemplate;

    /** GİB/entegratör uç noktası. Boş ise gönderim/sorgulama yapılamaz (sahte onay üretilmez). */
    @Value("${app.efatura.gib-endpoint:}")
    private String gibEndpoint;

    @Transactional(readOnly = true)
    public Page<EFaturaDTO> eFaturalariGetir(Long sirketId, Pageable pageable) {
        return eFaturaRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, pageable)
                .map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public EFaturaDTO eFaturaGetir(Long id) {
        EFatura eFatura = eFaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Fatura", id));
        tenantChecker.check(eFatura.getSirketId(), "E-Fatura");
        return entityToDTO(eFatura);
    }

    public EFaturaDTO eFaturaOlustur(Long faturaId, String senaryo, String tip, Long sirketId) {
        FaturaDTO fatura = faturaService.faturaGetir(faturaId);
        if (fatura == null) {
            throw new ResourceNotFoundException("Fatura", faturaId);
        }

        eFaturaRepository.findByFaturaId(faturaId).ifPresent(ef -> {
            throw new BusinessException("Bu fatura için zaten E-Fatura oluşturulmuş. ETTN: " + ef.getEttn());
        });

        String ettn = UUID.randomUUID().toString();
        String aliciVkn = null;
        if (fatura.getCariHesapId() != null) {
            try {
                aliciVkn = cariHesapRepository.findById(fatura.getCariHesapId())
                        .map(CariHesap::getVergiNumarasi).orElse(null);
            } catch (Exception e) {
                log.warn("Alıcı vergi no alınamadı: {}", e.getMessage());
            }
        }

        String ublXml = generateUblXml(fatura, ettn, senaryo, tip, sirketId, aliciVkn);

        EFatura eFatura = EFatura.builder()
                .faturaId(faturaId)
                .ettn(ettn)
                .faturaNo(fatura.getFaturaNumarasi())
                .senaryo(senaryo != null ? senaryo : "TEMELFATURA")
                .tip(tip != null ? tip : "SATIS")
                .gibDurumKodu(1000) // Hazırlandı
                .gibDurumAciklama("E-Fatura taslağı hazırlandı, GİB gönderimine hazır.")
                .aliciVknTckn(aliciVkn)
                .aliciUnvan(fatura.getCariHesapAd())
                .odenecekTutar(fatura.getGenelToplam())
                .ublXml(ublXml)
                .sirketId(sirketId)
                .build();

        EFatura saved = eFaturaRepository.save(eFatura);
        log.info("E-Fatura taslağı oluşturuldu - ETTN: {}, Fatura No: {}", ettn, fatura.getFaturaNumarasi());
        return entityToDTO(saved);
    }

    public EFaturaDTO gibGonder(Long id) {
        EFatura eFatura = eFaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Fatura", id));
        tenantChecker.check(eFatura.getSirketId(), "E-Fatura");

        if (eFatura.getGibDurumKodu() >= 1200) {
            throw new BusinessException("Bu E-Fatura zaten GİB'e gönderilmiş. Durum Kodu: " + eFatura.getGibDurumKodu());
        }

        boolean iletildi = false;
        if (gibEndpoint != null && !gibEndpoint.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                HttpEntity<String> entity = new HttpEntity<>(eFatura.getUblXml(), headers);
                restTemplate.postForEntity(gibEndpoint, entity, String.class);
                eFatura.setGibDurumKodu(1200);
                eFatura.setGibDurumAciklama("GİB'e iletildi (entegratör onay bekliyor).");
                iletildi = true;
                log.info("E-Fatura GİB uç noktasına iletildi - ETTN: {}", eFatura.getEttn());
            } catch (Exception ex) {
                log.warn("GİB uç noktasına gönderim başarısız: {}", ex.getMessage());
            }
        }

        if (!iletildi) {
            throw new BusinessException("E-Fatura GİB'e iletilemedi: uç nokta tanımlı değil veya gönderim başarısız. "
                    + "Gönderim gerçekleşmeden belge GİB'e iletilmiş olarak işaretlenemez.");
        }
        return entityToDTO(eFaturaRepository.save(eFatura));
    }

    public String xmlIndir(Long id) {
        EFatura eFatura = eFaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Fatura", id));
        tenantChecker.check(eFatura.getSirketId(), "E-Fatura");
        return eFatura.getUblXml();
    }

    /**
     * GİB/entegratörden güncel durum kodunu sorgular ve kaydı günceller.
     * Entegratör uç noktası tanımlı değilse simülasyon yapılmaz; açık hata döner.
     */
    public EFaturaDTO durumSorgula(Long id) {
        EFatura eFatura = eFaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Fatura", id));
        tenantChecker.check(eFatura.getSirketId(), "E-Fatura");

        if (eFatura.getGibDurumKodu() < 1200) {
            throw new BusinessException("E-Fatura henüz GİB'e gönderilmemiş. Önce GİB'e gönderin.");
        }
        if (eFatura.getGibDurumKodu() == 1300 || eFatura.getGibDurumKodu() == 1350) {
            return entityToDTO(eFatura); // zaten nihai durumda
        }
        if (gibEndpoint == null || gibEndpoint.isBlank()) {
            throw new BusinessException("GİB entegratör uç noktası tanımlı değil. Durum sorgulaması yapılamaz.");
        }

        Integer yeniKod = null;
        String yeniAciklama = null;
        try {
            String sorguUrl = gibEndpoint.endsWith("/")
                    ? gibEndpoint + eFatura.getEttn() + "/durum"
                    : gibEndpoint + "/" + eFatura.getEttn() + "/durum";
            var yanit = restTemplate.getForEntity(sorguUrl, java.util.Map.class);
            if (yanit.getBody() != null && yanit.getBody().get("durumKodu") instanceof Number n) {
                yeniKod = n.intValue();
                yeniAciklama = (String) yanit.getBody().get("durumAciklama");
            }
        } catch (Exception ex) {
            log.warn("GİB durum sorgulama başarısız: {}", ex.getMessage());
        }

        if (yeniKod == null) {
            throw new BusinessException("GİB durum sorgulaması başarısız: entegratörden geçerli yanıt alınamadı.");
        }

        eFatura.setGibDurumKodu(yeniKod);
        eFatura.setGibDurumAciklama(yeniAciklama);
        log.info("E-Fatura durumu güncellendi - ETTN: {}, Yeni Kod: {}", eFatura.getEttn(), yeniKod);
        return entityToDTO(eFaturaRepository.save(eFatura));
    }

    private String generateUblXml(FaturaDTO fatura, String ettn, String senaryo, String tip, Long sirketId, String aliciVkn) {
        String aliciUnvan = fatura.getCariHesapAd() != null ? fatura.getCariHesapAd() : "ALICI";
        String aliciVknTckn = aliciVkn != null && !aliciVkn.isBlank() ? aliciVkn : "11111111111";

        Sirket sirket = null;
        try {
            if (sirketId != null) sirket = sirketRepository.findById(sirketId).orElse(null);
        } catch (Exception e) {
            log.warn("Satıcı şirket bilgisi alınamadı: {}", e.getMessage());
        }
        String saticiUnvan = sirket != null ? sirket.getAd() : "SATICI";
        String saticiVkn = sirket != null && sirket.getVergiNo() != null ? sirket.getVergiNo() : "22222222222";
        String saticiAdres = sirket != null && sirket.getAdres() != null ? sirket.getAdres() : "İSTANBUL";
        String doviz = fatura.getParaBirimi() != null && !fatura.getParaBirimi().isBlank() ? fatura.getParaBirimi() : "TRY";

        // Satır bazında KDV-dahil modelden net/KDV ayrıştır (tek kanonik kaynak).
        List<com.raspel.erp.util.FaturaTutar.Satir> satirlar = new ArrayList<>();
        if (fatura.getKalemler() != null) {
            for (FaturaKalemDTO k : fatura.getKalemler()) {
                satirlar.add(com.raspel.erp.util.FaturaTutar.satir(
                        k.getBirimFiyat(), k.getAdet(), k.getIskontoOrani(), k.getKdvOrani()));
            }
        }
        BigDecimal netToplam = fatura.getAraToplam() != null ? fatura.getAraToplam()
                : satirlar.stream().map(com.raspel.erp.util.FaturaTutar.Satir::net).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal kdvToplam = fatura.getKdv() != null ? fatura.getKdv()
                : satirlar.stream().map(com.raspel.erp.util.FaturaTutar.Satir::kdv).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal genelToplam = fatura.getGenelToplam() != null ? fatura.getGenelToplam() : netToplam.add(kdvToplam);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n");
        xml.append("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        xml.append("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");
        xml.append("    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        xml.append("    <cbc:CustomizationID>TR1.2</cbc:CustomizationID>\n");
        xml.append("    <cbc:ProfileID>").append(esc(senaryo != null ? senaryo : "TEMELFATURA")).append("</cbc:ProfileID>\n");
        xml.append("    <cbc:ID>").append(esc(fatura.getFaturaNumarasi())).append("</cbc:ID>\n");
        xml.append("    <cbc:UUID>").append(esc(ettn)).append("</cbc:UUID>\n");
        xml.append("    <cbc:IssueDate>").append(fatura.getTarih()).append("</cbc:IssueDate>\n");
        xml.append("    <cbc:InvoiceTypeCode>").append(esc(tip != null ? tip : "SATIS")).append("</cbc:InvoiceTypeCode>\n");
        xml.append("    <cbc:DocumentCurrencyCode>").append(esc(doviz)).append("</cbc:DocumentCurrencyCode>\n");

        xml.append("    <cac:AccountingSupplierParty>\n");
        xml.append("        <cac:Party>\n");
        xml.append("            <cac:PartyIdentification><cbc:ID schemeID=\"VKN\">").append(esc(saticiVkn)).append("</cbc:ID></cac:PartyIdentification>\n");
        xml.append("            <cac:PartyName><cbc:Name>").append(esc(saticiUnvan)).append("</cbc:Name></cac:PartyName>\n");
        xml.append("            <cac:PostalAddress><cbc:CityName>İSTANBUL</cbc:CityName><cbc:StreetName>").append(esc(saticiAdres)).append("</cbc:StreetName></cac:PostalAddress>\n");
        xml.append("        </cac:Party>\n");
        xml.append("    </cac:AccountingSupplierParty>\n");

        xml.append("    <cac:AccountingCustomerParty>\n");
        xml.append("        <cac:Party>\n");
        xml.append("            <cac:PartyIdentification><cbc:ID schemeID=\"VKN\">").append(esc(aliciVknTckn)).append("</cbc:ID></cac:PartyIdentification>\n");
        xml.append("            <cac:PartyName><cbc:Name>").append(esc(aliciUnvan)).append("</cbc:Name></cac:PartyName>\n");
        xml.append("        </cac:Party>\n");
        xml.append("    </cac:AccountingCustomerParty>\n");

        // Belge düzeyi KDV toplamı.
        xml.append("    <cac:TaxTotal>\n");
        xml.append("        <cbc:TaxAmount currencyID=\"").append(esc(doviz)).append("\">").append(kdvToplam.toPlainString()).append("</cbc:TaxAmount>\n");
        xml.append("    </cac:TaxTotal>\n");

        if (fatura.getKalemler() != null) {
            int sira = 1;
            for (FaturaKalemDTO k : fatura.getKalemler()) {
                String aciklama = k.getAciklama() != null ? k.getAciklama() : ("Kalem " + sira);
                com.raspel.erp.util.FaturaTutar.Satir s = satirlar.get(sira - 1);
                BigDecimal adet = k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO;
                BigDecimal netBirim = adet.signum() > 0
                        ? s.net().divide(adet, 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.ZERO;

                xml.append("    <cac:InvoiceLine>\n");
                xml.append("        <cbc:ID>").append(sira).append("</cbc:ID>\n");
                xml.append("        <cbc:InvoicedQuantity unitCode=\"C62\">").append(adet.toPlainString()).append("</cbc:InvoicedQuantity>\n");
                xml.append("        <cbc:LineExtensionAmount currencyID=\"").append(esc(doviz)).append("\">").append(s.net().toPlainString()).append("</cbc:LineExtensionAmount>\n");
                xml.append("        <cac:TaxTotal>\n");
                xml.append("            <cbc:TaxAmount currencyID=\"").append(esc(doviz)).append("\">").append(s.kdv().toPlainString()).append("</cbc:TaxAmount>\n");
                xml.append("            <cac:TaxSubtotal>\n");
                xml.append("                <cbc:TaxableAmount currencyID=\"").append(esc(doviz)).append("\">").append(s.net().toPlainString()).append("</cbc:TaxableAmount>\n");
                xml.append("                <cbc:TaxAmount currencyID=\"").append(esc(doviz)).append("\">").append(s.kdv().toPlainString()).append("</cbc:TaxAmount>\n");
                xml.append("                <cbc:Percent>").append(kdvOrani.toPlainString()).append("</cbc:Percent>\n");
                xml.append("            </cac:TaxSubtotal>\n");
                xml.append("        </cac:TaxTotal>\n");
                xml.append("        <cac:Item><cbc:Name>").append(esc(aciklama)).append("</cbc:Name></cac:Item>\n");
                xml.append("        <cac:Price><cbc:PriceAmount currencyID=\"").append(esc(doviz)).append("\">").append(netBirim.toPlainString()).append("</cbc:PriceAmount></cac:Price>\n");
                xml.append("    </cac:InvoiceLine>\n");
                sira++;
            }
        }

        xml.append("    <cac:LegalMonetaryTotal>\n");
        xml.append("        <cbc:LineExtensionAmount currencyID=\"").append(esc(doviz)).append("\">").append(netToplam.toPlainString()).append("</cbc:LineExtensionAmount>\n");
        xml.append("        <cbc:TaxExclusiveAmount currencyID=\"").append(esc(doviz)).append("\">").append(netToplam.toPlainString()).append("</cbc:TaxExclusiveAmount>\n");
        xml.append("        <cbc:TaxInclusiveAmount currencyID=\"").append(esc(doviz)).append("\">").append(genelToplam.toPlainString()).append("</cbc:TaxInclusiveAmount>\n");
        xml.append("        <cbc:PayableAmount currencyID=\"").append(esc(doviz)).append("\">").append(genelToplam.toPlainString()).append("</cbc:PayableAmount>\n");
        xml.append("    </cac:LegalMonetaryTotal>\n");
        xml.append("</Invoice>");
        return xml.toString();
    }

    /** XML özel karakterlerini kaçırır (metin/attribute güvenliği). */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private EFaturaDTO entityToDTO(EFatura ef) {
        return EFaturaDTO.builder()
                .id(ef.getId())
                .faturaId(ef.getFaturaId())
                .ettn(ef.getEttn())
                .faturaNo(ef.getFaturaNo())
                .senaryo(ef.getSenaryo())
                .tip(ef.getTip())
                .gibDurumKodu(ef.getGibDurumKodu())
                .gibDurumAciklama(ef.getGibDurumAciklama())
                .aliciVknTckn(ef.getAliciVknTckn())
                .aliciUnvan(ef.getAliciUnvan())
                .odenecekTutar(ef.getOdenecekTutar())
                .ublXml(ef.getUblXml())
                .sirketId(ef.getSirketId())
                .olusturmaTarihi(ef.getOlusturmaTarihi())
                .build();
    }
}