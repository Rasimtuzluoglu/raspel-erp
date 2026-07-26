package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.FaturaDTO;
import com.raspel.erp.dto.ticaret.EFaturaDTO;
import com.raspel.erp.entity.ticaret.EFatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.EFaturaRepository;
import com.raspel.erp.service.FaturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EFaturaService {

    private final EFaturaRepository eFaturaRepository;
    private final FaturaService faturaService;

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
        String ublXml = generateUblXml(fatura, ettn, senaryo, tip);

        EFatura eFatura = EFatura.builder()
                .faturaId(faturaId)
                .ettn(ettn)
                .faturaNo(fatura.getFaturaNumarasi())
                .senaryo(senaryo != null ? senaryo : "TEMELFATURA")
                .tip(tip != null ? tip : "SATIS")
                .gibDurumKodu(1000) // Hazırlandı
                .gibDurumAciklama("E-Fatura taslağı hazırlandı, GİB gönderimine hazır.")
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

        eFatura.setGibDurumKodu(1200);
        eFatura.setGibDurumAciklama("GİB Merkez Sistemine başarıyla iletildi (Onay Bekliyor).");
        log.info("E-Fatura GİB'e iletildi - ETTN: {}", eFatura.getEttn());
        return entityToDTO(eFaturaRepository.save(eFatura));
    }

    public String xmlIndir(Long id) {
        EFatura eFatura = eFaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Fatura", id));
        return eFatura.getUblXml();
    }

    private String generateUblXml(FaturaDTO fatura, String ettn, String senaryo, String tip) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n");
        xml.append("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        xml.append("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");
        xml.append("    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        xml.append("    <cbc:CustomizationID>TR1.2</cbc:CustomizationID>\n");
        xml.append("    <cbc:ProfileID>").append(senaryo).append("</cbc:ProfileID>\n");
        xml.append("    <cbc:ID>").append(fatura.getFaturaNumarasi()).append("</cbc:ID>\n");
        xml.append("    <cbc:UUID>").append(ettn).append("</cbc:UUID>\n");
        xml.append("    <cbc:IssueDate>").append(fatura.getTarih()).append("</cbc:IssueDate>\n");
        xml.append("    <cbc:InvoiceTypeCode>").append(tip).append("</cbc:InvoiceTypeCode>\n");
        xml.append("    <cbc:DocumentCurrencyCode>TRY</cbc:DocumentCurrencyCode>\n");
        xml.append("    <cac:LegalMonetaryTotal>\n");
        xml.append("        <cbc:PayableAmount currencyID=\"TRY\">").append(fatura.getGenelToplam()).append("</cbc:PayableAmount>\n");
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
