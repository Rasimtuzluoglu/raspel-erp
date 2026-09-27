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
    private final com.raspel.erp.repository.ticaret.IadeRepository iadeRepository;
    private final com.raspel.erp.repository.ticaret.IadeKalemRepository iadeKalemRepository;
    private final EEntegratorAdaptoru entegratorAdaptoru;

    /** GİB/entegratör uç noktası. Boş ise gönderim/sorgulama yapılamaz (sahte onay üretilmez). */
    @Value("${app.efatura.gib-endpoint:}")
    private String gibEndpoint;

    /**
     * E-Fatura zorunluluk eşiği (KDV hariç tutar). Bu tutarın üzerindeki satışlar e-Fatura,
     * altındakiler e-Arşiv kapsamındadır. 2026 için 3.000 TL varsayılan.
     */
    @Value("${app.efatura.esik-tutar:3000}")
    private BigDecimal efaturaEsikTutar;

    /**
     * Fatura tutarına göre varsayılan e-belge senaryosunu çözer. Çağıran açıkça senaryo
     * vermediyse kullanılır: eşik üstü TEMELFATURA, eşik altı EARSIVEFATURA.
     */
    private String senaryoCoz(String verilenSenaryo, FaturaDTO fatura) {
        if (verilenSenaryo != null && !verilenSenaryo.isBlank()) {
            return verilenSenaryo;
        }
        BigDecimal matrah = fatura.getAraToplam() != null ? fatura.getAraToplam()
                : (fatura.getGenelToplam() != null ? fatura.getGenelToplam() : BigDecimal.ZERO);
        return matrah.compareTo(efaturaEsikTutar) >= 0 ? "TEMELFATURA" : "EARSIVEFATURA";
    }

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

        String senaryoKarar = senaryoCoz(senaryo, fatura);
        String tipKarar = tip != null ? tip : "SATIS";
        String ublXml = generateUblXml(fatura, ettn, senaryoKarar, tipKarar, sirketId, aliciVkn);

        EFatura eFatura = EFatura.builder()
                .faturaId(faturaId)
                .ettn(ettn)
                .faturaNo(fatura.getFaturaNumarasi())
                .senaryo(senaryoKarar)
                .tip(tipKarar)
                .belgeTuru(belgeTuruCoz(senaryoKarar, tipKarar))
                .gibDurumKodu(1000) // Hazırlandı
                .gibDurumAciklama("EARSIVEFATURA".equals(senaryoKarar)
                        ? "E-Arşiv taslağı hazırlandı (e-Fatura eşiği altı)."
                        : "E-Fatura taslağı hazırlandı, GİB gönderimine hazır.")
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

    /** Senaryo ve tipe göre belge türünü çözer: EFATURA, EARSIV veya EIADE. */
    private String belgeTuruCoz(String senaryo, String tip) {
        if ("IADE".equalsIgnoreCase(tip)) return "EIADE";
        return senaryo != null && senaryo.toUpperCase().contains("EARSIV") ? "EARSIV" : "EFATURA";
    }

    /**
     * Kredi notu (e-Arşiv iade belgesi) üretir: tamamlanmış iade kaydından e-Arşiv
     * senaryosunda IADE tipinde e-belge oluşturur ve iadeye bağlar. Aynı iade için
     * mükerrer belge üretilmez.
     */
    public EFaturaDTO krediNotuOlustur(Long iadeId, Long sirketId) {
        var iade = iadeRepository.findById(iadeId)
                .orElseThrow(() -> new ResourceNotFoundException("İade", iadeId));
        tenantChecker.check(iade.getSirketId(), "İade");
        if (!"TAMAMLANDI".equals(iade.getDurum())) {
            throw new BusinessException("Yalnızca tamamlanmış iadeler için kredi notu düzenlenebilir");
        }
        if (iade.getFaturaId() != null) {
            eFaturaRepository.findByFaturaId(iade.getFaturaId()).ifPresent(ef -> {
                if ("EIADE".equals(ef.getBelgeTuru())) {
                    throw new BusinessException("Bu iade için zaten kredi notu oluşturulmuş. ETTN: " + ef.getEttn());
                }
            });
        }

        String ettn = UUID.randomUUID().toString();
        String aliciVkn = null;
        String aliciUnvan = null;
        if (iade.getCariHesapId() != null) {
            var cari = cariHesapRepository.findById(iade.getCariHesapId()).orElse(null);
            if (cari != null) {
                aliciVkn = cari.getVergiNumarasi();
                aliciUnvan = cari.getAd();
            }
        }

        // İade kalemlerinden UBL üretimi için fatura benzeri DTO kurulur.
        FaturaDTO iadeDto = new FaturaDTO();
        iadeDto.setFaturaNumarasi("IADE-" + iade.getId());
        iadeDto.setTarih(iade.getTarih());
        iadeDto.setCariHesapId(iade.getCariHesapId());
        iadeDto.setCariHesapAd(aliciUnvan);
        iadeDto.setParaBirimi("TRY");
        List<FaturaKalemDTO> kalemler = new ArrayList<>();
        for (var k : iadeKalemRepository.findByIadeId(iade.getId())) {
            kalemler.add(FaturaKalemDTO.builder()
                    .aciklama(k.getAciklama()).adet(k.getMiktar())
                    .birimFiyat(k.getBirimFiyat()).kdvOrani(k.getKdvOrani())
                    .build());
        }
        iadeDto.setKalemler(kalemler);
        iadeDto.setGenelToplam(iade.getTutar());

        String ublXml = generateUblXml(iadeDto, ettn, "EARSIVEFATURA", "IADE", sirketId, aliciVkn);

        EFatura eFatura = EFatura.builder()
                .faturaId(iade.getFaturaId())
                .iadeId(iade.getId())
                .ettn(ettn)
                .faturaNo("IADE-" + iade.getId())
                .senaryo("EARSIVEFATURA")
                .tip("IADE")
                .belgeTuru("EIADE")
                .gibDurumKodu(1000)
                .gibDurumAciklama("Kredi notu (e-Arşiv iade) taslağı hazırlandı.")
                .aliciVknTckn(aliciVkn)
                .aliciUnvan(aliciUnvan)
                .odenecekTutar(iade.getTutar())
                .ublXml(ublXml)
                .sirketId(sirketId != null ? sirketId : iade.getSirketId())
                .build();
        EFatura saved = eFaturaRepository.save(eFatura);
        log.info("Kredi notu taslağı oluşturuldu - ETTN: {}, İade ID: {}", ettn, iadeId);
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
        } else if (entegratorAdaptoru.tanimliMi()) {
            // Doğrudan uç nokta tanımlı değilse entegratör adaptörü üzerinden gönderilir.
            if (entegratorAdaptoru.gonder(eFatura.getEttn(), eFatura.getBelgeTuru(), eFatura.getUblXml())) {
                eFatura.setGibDurumKodu(1200);
                eFatura.setGibDurumAciklama("GİB'e iletildi (entegratör onay bekliyor).");
                iletildi = true;
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
            // Doğrudan uç nokta yoksa entegratör adaptörü denenir.
            if (entegratorAdaptoru.tanimliMi()) {
                Integer kod = entegratorAdaptoru.durumSorgula(eFatura.getEttn());
                if (kod == null) {
                    throw new BusinessException("GİB durum sorgulaması başarısız: entegratörden geçerli yanıt alınamadı.");
                }
                eFatura.setGibDurumKodu(kod);
                eFatura.setGibDurumAciklama("Entegratör durumu");
                log.info("E-Fatura durumu (entegratör) güncellendi - ETTN: {}, Kod: {}", eFatura.getEttn(), kod);
                return entityToDTO(eFaturaRepository.save(eFatura));
            }
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
        // GİB e-Arşiv profil kodu EARSIVFATURA'dır (senaryo adındaki fazladan E düzeltilir).
        String profil = senaryo != null && senaryo.toUpperCase().contains("EARSIV")
                ? "EARSIVFATURA" : (senaryo != null ? senaryo : "TEMELFATURA");
        xml.append("    <cbc:ProfileID>").append(esc(profil)).append("</cbc:ProfileID>\n");
        xml.append("    <cbc:ID>").append(esc(fatura.getFaturaNumarasi())).append("</cbc:ID>\n");
        xml.append("    <cbc:UUID>").append(esc(ettn)).append("</cbc:UUID>\n");
        java.time.LocalDate belgeTarihi = fatura.getTarih() != null ? fatura.getTarih() : java.time.LocalDate.now();
        xml.append("    <cbc:IssueDate>").append(belgeTarihi).append("</cbc:IssueDate>\n");
        xml.append("    <cbc:IssueTime>").append(java.time.LocalTime.now().withNano(0)).append("</cbc:IssueTime>\n");
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
                .belgeTuru(ef.getBelgeTuru())
                .iadeId(ef.getIadeId())
                .sirketId(ef.getSirketId())
                .olusturmaTarihi(ef.getOlusturmaTarihi())
                .build();
    }
}