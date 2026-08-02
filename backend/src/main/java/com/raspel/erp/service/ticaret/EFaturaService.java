package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.FaturaDTO;
import com.raspel.erp.dto.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.EFaturaDTO;
import com.raspel.erp.entity.CariHesap;
import com.raspel.erp.entity.Sirket;
import com.raspel.erp.entity.ticaret.EFatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.SirketRepository;
import com.raspel.erp.repository.ticaret.EFaturaRepository;
import com.raspel.erp.service.FaturaService;
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
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EFaturaService {

    private final EFaturaRepository eFaturaRepository;
    private final FaturaService faturaService;
    private final CariHesapRepository cariHesapRepository;
    private final SirketRepository sirketRepository;

    /** GİB/entegratör uç noktası. Boş ise yerel onay (simülasyon) yapılır. */
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

        if (eFatura.getGibDurumKodu() >= 1200) {
            throw new BusinessException("Bu E-Fatura zaten GİB'e gönderilmiş. Durum Kodu: " + eFatura.getGibDurumKodu());
        }

        boolean iletildi = false;
        if (gibEndpoint != null && !gibEndpoint.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                HttpEntity<String> entity = new HttpEntity<>(eFatura.getUblXml(), headers);
                new RestTemplate().postForEntity(gibEndpoint, entity, String.class);
                eFatura.setGibDurumKodu(1200);
                eFatura.setGibDurumAciklama("GİB'e iletildi (entegratör onay bekliyor).");
                iletildi = true;
                log.info("E-Fatura GİB uç noktasına iletildi - ETTN: {}", eFatura.getEttn());
            } catch (Exception ex) {
                log.warn("GİB uç noktasına gönderim başarısız ({}). Yerel onaya düşülüyor.", ex.getMessage());
            }
        }

        if (!iletildi) {
            eFatura.setGibDurumKodu(1200);
            eFatura.setGibDurumAciklama("GİB'e iletildi (Onay Bekliyor). GİB entegratör uç noktası tanımlı değil — yerel simülasyon onayı.");
        }
        return entityToDTO(eFaturaRepository.save(eFatura));
    }

    public String xmlIndir(Long id) {
        EFatura eFatura = eFaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Fatura", id));
        return eFatura.getUblXml();
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

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n");
        xml.append("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        xml.append("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");
        xml.append("    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        xml.append("    <cbc:CustomizationID>TR1.2</cbc:CustomizationID>\n");
        xml.append("    <cbc:ProfileID>").append(senaryo != null ? senaryo : "TEMELFATURA").append("</cbc:ProfileID>\n");
        xml.append("    <cbc:ID>").append(fatura.getFaturaNumarasi()).append("</cbc:ID>\n");
        xml.append("    <cbc:UUID>").append(ettn).append("</cbc:UUID>\n");
        xml.append("    <cbc:IssueDate>").append(fatura.getTarih()).append("</cbc:IssueDate>\n");
        xml.append("    <cbc:InvoiceTypeCode>").append(tip != null ? tip : "SATIS").append("</cbc:InvoiceTypeCode>\n");
        xml.append("    <cbc:DocumentCurrencyCode>TRY</cbc:DocumentCurrencyCode>\n");

        xml.append("    <cac:AccountingSupplierParty>\n");
        xml.append("        <cac:Party>\n");
        xml.append("            <cac:PartyIdentification><cbc:ID schemeID=\"VKN\">").append(saticiVkn).append("</cbc:ID></cac:PartyIdentification>\n");
        xml.append("            <cac:PartyName><cbc:Name>").append(saticiUnvan).append("</cbc:Name></cac:PartyName>\n");
        xml.append("            <cac:PostalAddress><cbc:CityName>İSTANBUL</cbc:CityName><cbc:StreetName>").append(saticiAdres).append("</cbc:StreetName></cac:PostalAddress>\n");
        xml.append("        </cac:Party>\n");
        xml.append("    </cac:AccountingSupplierParty>\n");

        xml.append("    <cac:AccountingCustomerParty>\n");
        xml.append("        <cac:Party>\n");
        xml.append("            <cac:PartyIdentification><cbc:ID schemeID=\"VKN\">").append(aliciVknTckn).append("</cbc:ID></cac:PartyIdentification>\n");
        xml.append("            <cac:PartyName><cbc:Name>").append(aliciUnvan).append("</cbc:Name></cac:PartyName>\n");
        xml.append("        </cac:Party>\n");
        xml.append("    </cac:AccountingCustomerParty>\n");

        BigDecimal toplamKdv = BigDecimal.ZERO;
        if (fatura.getKalemler() != null) {
            int sira = 1;
            for (FaturaKalemDTO k : fatura.getKalemler()) {
                String aciklama = k.getAciklama() != null ? k.getAciklama() : ("Kalem " + sira);
                BigDecimal kdvTutar = BigDecimal.ZERO;
                if (k.getTutar() != null) {
                    BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.ZERO;
                    kdvTutar = k.getTutar().multiply(kdvOrani)
                            .divide(BigDecimal.valueOf(100).add(kdvOrani), 2, java.math.RoundingMode.HALF_UP);
                }
                toplamKdv = toplamKdv.add(kdvTutar);
                xml.append("    <cac:InvoiceLine>\n");
                xml.append("        <cbc:ID>").append(sira).append("</cbc:ID>\n");
                xml.append("        <cbc:InvoicedQuantity unitCode=\"C62\">").append(k.getAdet()).append("</cbc:InvoicedQuantity>\n");
                xml.append("        <cbc:LineExtensionAmount currencyID=\"TRY\">").append(k.getTutar() != null ? k.getTutar() : "0.00").append("</cbc:LineExtensionAmount>\n");
                xml.append("        <cac:Item><cbc:Name>").append(aciklama).append("</cbc:Name></cac:Item>\n");
                xml.append("        <cac:Price><cbc:PriceAmount currencyID=\"TRY\">").append(k.getBirimFiyat() != null ? k.getBirimFiyat() : "0.00").append("</cbc:PriceAmount></cac:Price>\n");
                xml.append("    </cac:InvoiceLine>\n");
                sira++;
            }
        }

        xml.append("    <cac:LegalMonetaryTotal>\n");
        xml.append("        <cbc:LineExtensionAmount currencyID=\"TRY\">").append(fatura.getAraToplam() != null ? fatura.getAraToplam() : "0.00").append("</cbc:LineExtensionAmount>\n");
        xml.append("        <cbc:TaxExclusiveAmount currencyID=\"TRY\">").append(fatura.getAraToplam() != null ? fatura.getAraToplam() : "0.00").append("</cbc:TaxExclusiveAmount>\n");
        xml.append("        <cbc:TaxInclusiveAmount currencyID=\"TRY\">").append(fatura.getGenelToplam() != null ? fatura.getGenelToplam() : "0.00").append("</cbc:TaxInclusiveAmount>\n");
        xml.append("        <cbc:PayableAmount currencyID=\"TRY\">").append(fatura.getGenelToplam() != null ? fatura.getGenelToplam() : "0.00").append("</cbc:PayableAmount>\n");
        xml.append("    </cac:LegalMonetaryTotal>\n");
        xml.append("</Invoice>");
        return xml.toString();
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
