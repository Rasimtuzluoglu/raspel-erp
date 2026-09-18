package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.CrmAktiviteDTO;
import com.raspel.erp.dto.ticaret.CrmKampanyaDTO;
import com.raspel.erp.dto.ticaret.CrmLeadDTO;
import com.raspel.erp.entity.ticaret.CrmAktivite;
import com.raspel.erp.entity.ticaret.CrmKampanya;
import com.raspel.erp.entity.ticaret.CrmLead;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.CrmAktiviteRepository;
import com.raspel.erp.repository.ticaret.CrmKampanyaRepository;
import com.raspel.erp.repository.ticaret.CrmLeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRM genişletmesi: lead, aktivite ve kampanya yönetimi. Fırsat/lead'ler
 * kampanyalara bağlanabilir; aktiviteler cari/fırsat/lead ile ilişkilendirilir.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CrmGenisletmeService {

    private final CrmLeadRepository crmLeadRepository;
    private final CrmAktiviteRepository crmAktiviteRepository;
    private final CrmKampanyaRepository crmKampanyaRepository;
    private final CariHesapRepository cariHesapRepository;
    private final TenantChecker tenantChecker;

    // ------------------------------------------------------------------
    // Lead
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CrmLeadDTO> leadleriGetir(Long sirketId, String durum, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        if (durum != null && !durum.isBlank()) {
            var liste = crmLeadRepository.findBySirketIdAndDurumOrderBySkorDesc(sirketId, durum);
            return new org.springframework.data.domain.PageImpl<>(
                    leadMapDTO(liste), pageable, liste.size());
        }
        return crmLeadRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, pageable)
                .map(this::leadToDTO);
    }

    @Transactional(readOnly = true)
    public CrmLeadDTO leadGetir(Long id) {
        CrmLead l = crmLeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Lead", id));
        tenantChecker.check(l.getSirketId(), "CRM Lead");
        return leadToDTO(l);
    }

    public CrmLeadDTO leadOlustur(CrmLeadDTO dto, Long sirketId) {
        if (sirketId == null) throw new BusinessException("Şirket bilgisi zorunludur");
        CrmLead lead = CrmLead.builder()
                .sirketId(sirketId)
                .ad(dto.getAd())
                .firma(dto.getFirma())
                .email(dto.getEmail())
                .telefon(dto.getTelefon())
                .kaynak(dto.getKaynak())
                .durum(dto.getDurum() != null ? dto.getDurum() : "YENI")
                .skor(dto.getSkor() != null ? dto.getSkor() : 0)
                .tahminiDeger(dto.getTahminiDeger())
                .cariHesapId(dto.getCariHesapId())
                .kampanyaId(dto.getKampanyaId())
                .aciklama(dto.getAciklama())
                .kullaniciId(dto.getKullaniciId() != null ? dto.getKullaniciId() : tenantChecker.getCurrentKullaniciId())
                .build();
        return leadToDTO(crmLeadRepository.save(lead));
    }

    public CrmLeadDTO leadGuncelle(Long id, CrmLeadDTO dto) {
        CrmLead l = crmLeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Lead", id));
        tenantChecker.check(l.getSirketId(), "CRM Lead");
        if (dto.getAd() != null) l.setAd(dto.getAd());
        if (dto.getFirma() != null) l.setFirma(dto.getFirma());
        if (dto.getEmail() != null) l.setEmail(dto.getEmail());
        if (dto.getTelefon() != null) l.setTelefon(dto.getTelefon());
        if (dto.getKaynak() != null) l.setKaynak(dto.getKaynak());
        if (dto.getDurum() != null) l.setDurum(dto.getDurum());
        if (dto.getSkor() != null) l.setSkor(dto.getSkor());
        if (dto.getTahminiDeger() != null) l.setTahminiDeger(dto.getTahminiDeger());
        if (dto.getCariHesapId() != null) l.setCariHesapId(dto.getCariHesapId());
        if (dto.getKampanyaId() != null) l.setKampanyaId(dto.getKampanyaId());
        if (dto.getAciklama() != null) l.setAciklama(dto.getAciklama());
        l.setGuncellemeTarihi(LocalDateTime.now());
        return leadToDTO(crmLeadRepository.save(l));
    }

    public void leadSil(Long id) {
        CrmLead l = crmLeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Lead", id));
        tenantChecker.check(l.getSirketId(), "CRM Lead");
        crmLeadRepository.delete(l);
    }

    /** Lead'i mevcut bir cari hesaba dönüştürür ve durumu günceller. */
    public CrmLeadDTO leadDonustur(Long id, Long cariHesapId) {
        CrmLead l = crmLeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Lead", id));
        tenantChecker.check(l.getSirketId(), "CRM Lead");
        if (cariHesapId == null) throw new BusinessException("Dönüştürülecek cari hesap seçilmelidir");
        var cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("CariHesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "CariHesap");
        l.setCariHesapId(cariHesapId);
        l.setDurum("DONUSTURULDU");
        l.setGuncellemeTarihi(LocalDateTime.now());
        return leadToDTO(crmLeadRepository.save(l));
    }

    // ------------------------------------------------------------------
    // Aktivite
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CrmAktiviteDTO> aktiviteleriGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        return crmAktiviteRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, pageable)
                .map(this::aktiviteToDTO);
    }

    @Transactional(readOnly = true)
    public java.util.List<CrmAktiviteDTO> cariAktiviteleri(Long sirketId, Long cariHesapId) {
        if (sirketId == null) return java.util.List.of();
        return crmAktiviteRepository.findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(sirketId, cariHesapId)
                .stream().map(this::aktiviteToDTO).collect(Collectors.toList());
    }

    public CrmAktiviteDTO aktiviteOlustur(CrmAktiviteDTO dto, Long sirketId) {
        if (sirketId == null) throw new BusinessException("Şirket bilgisi zorunludur");
        CrmAktivite a = CrmAktivite.builder()
                .sirketId(sirketId)
                .tur(dto.getTur() != null ? dto.getTur() : "NOT")
                .baslik(dto.getBaslik())
                .aciklama(dto.getAciklama())
                .cariHesapId(dto.getCariHesapId())
                .firsatId(dto.getFirsatId())
                .leadId(dto.getLeadId())
                .planlananTarih(dto.getPlanlananTarih())
                .tamamlandi(dto.getTamamlandi() != null && dto.getTamamlandi())
                .kullaniciId(dto.getKullaniciId() != null ? dto.getKullaniciId() : tenantChecker.getCurrentKullaniciId())
                .build();
        if (Boolean.TRUE.equals(a.getTamamlandi())) a.setTamamlanmaTarihi(LocalDateTime.now());
        return aktiviteToDTO(crmAktiviteRepository.save(a));
    }

    public CrmAktiviteDTO aktiviteTamamla(Long id, boolean tamamlandi) {
        CrmAktivite a = crmAktiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Aktivite", id));
        tenantChecker.check(a.getSirketId(), "CRM Aktivite");
        a.setTamamlandi(tamamlandi);
        a.setTamamlanmaTarihi(tamamlandi ? LocalDateTime.now() : null);
        return aktiviteToDTO(crmAktiviteRepository.save(a));
    }

    public void aktiviteSil(Long id) {
        CrmAktivite a = crmAktiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Aktivite", id));
        tenantChecker.check(a.getSirketId(), "CRM Aktivite");
        crmAktiviteRepository.delete(a);
    }

    // ------------------------------------------------------------------
    // Kampanya
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CrmKampanyaDTO> kampanyalariGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        return crmKampanyaRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, pageable)
                .map(this::kampanyaToDTO);
    }

    public CrmKampanyaDTO kampanyaOlustur(CrmKampanyaDTO dto, Long sirketId) {
        if (sirketId == null) throw new BusinessException("Şirket bilgisi zorunludur");
        CrmKampanya k = CrmKampanya.builder()
                .sirketId(sirketId)
                .ad(dto.getAd())
                .tur(dto.getTur())
                .durum(dto.getDurum() != null ? dto.getDurum() : "PLANLANDI")
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .butce(dto.getButce())
                .harcama(dto.getHarcama())
                .hedefKisi(dto.getHedefKisi())
                .aciklama(dto.getAciklama())
                .build();
        return kampanyaToDTO(crmKampanyaRepository.save(k));
    }

    public CrmKampanyaDTO kampanyaGuncelle(Long id, CrmKampanyaDTO dto) {
        CrmKampanya k = crmKampanyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Kampanya", id));
        tenantChecker.check(k.getSirketId(), "CRM Kampanya");
        if (dto.getAd() != null) k.setAd(dto.getAd());
        if (dto.getTur() != null) k.setTur(dto.getTur());
        if (dto.getDurum() != null) k.setDurum(dto.getDurum());
        if (dto.getBaslangic() != null) k.setBaslangic(dto.getBaslangic());
        if (dto.getBitis() != null) k.setBitis(dto.getBitis());
        if (dto.getButce() != null) k.setButce(dto.getButce());
        if (dto.getHarcama() != null) k.setHarcama(dto.getHarcama());
        if (dto.getHedefKisi() != null) k.setHedefKisi(dto.getHedefKisi());
        if (dto.getAciklama() != null) k.setAciklama(dto.getAciklama());
        return kampanyaToDTO(crmKampanyaRepository.save(k));
    }

    public void kampanyaSil(Long id) {
        CrmKampanya k = crmKampanyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM Kampanya", id));
        tenantChecker.check(k.getSirketId(), "CRM Kampanya");
        crmKampanyaRepository.delete(k);
    }

    // ------------------------------------------------------------------
    // DTO / yardımcı
    // ------------------------------------------------------------------

    private java.util.List<CrmLeadDTO> leadMapDTO(java.util.List<CrmLead> liste) {
        Set<Long> cariIdler = liste.stream().map(CrmLead::getCariHesapId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> cariAdlari = cariIdler.isEmpty() ? Map.of()
                : cariHesapRepository.findAllById(cariIdler).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.finans.CariHesap::getId,
                                com.raspel.erp.entity.finans.CariHesap::getAd, (a, b) -> a));
        Set<Long> kampanyaIdler = liste.stream().map(CrmLead::getKampanyaId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> kampanyaAdlari = kampanyaIdler.isEmpty() ? Map.of()
                : crmKampanyaRepository.findAllById(kampanyaIdler).stream()
                        .collect(Collectors.toMap(CrmKampanya::getId, CrmKampanya::getAd, (a, b) -> a));
        return liste.stream().map(l -> leadToDTO(l, cariAdlari, kampanyaAdlari)).collect(Collectors.toList());
    }

    private CrmLeadDTO leadToDTO(CrmLead l) {
        String cariAd = l.getCariHesapId() != null
                ? cariHesapRepository.findById(l.getCariHesapId()).map(c -> c.getAd()).orElse(null) : null;
        String kampanyaAd = l.getKampanyaId() != null
                ? crmKampanyaRepository.findById(l.getKampanyaId()).map(CrmKampanya::getAd).orElse(null) : null;
        return leadToDTO(l, cariAd != null ? Map.of(l.getCariHesapId(), cariAd) : Map.of(),
                kampanyaAd != null ? Map.of(l.getKampanyaId(), kampanyaAd) : Map.of());
    }

    private CrmLeadDTO leadToDTO(CrmLead l, Map<Long, String> cariAdlari, Map<Long, String> kampanyaAdlari) {
        return CrmLeadDTO.builder()
                .id(l.getId()).sirketId(l.getSirketId()).ad(l.getAd())
                .firma(l.getFirma()).email(l.getEmail()).telefon(l.getTelefon())
                .kaynak(l.getKaynak()).durum(l.getDurum()).skor(l.getSkor())
                .tahminiDeger(l.getTahminiDeger())
                .cariHesapId(l.getCariHesapId())
                .cariHesapAd(l.getCariHesapId() != null ? cariAdlari.get(l.getCariHesapId()) : null)
                .firsatId(l.getFirsatId())
                .kampanyaId(l.getKampanyaId())
                .kampanyaAd(l.getKampanyaId() != null ? kampanyaAdlari.get(l.getKampanyaId()) : null)
                .aciklama(l.getAciklama()).kullaniciId(l.getKullaniciId())
                .olusturmaTarihi(l.getOlusturmaTarihi()).guncellemeTarihi(l.getGuncellemeTarihi())
                .build();
    }

    private CrmAktiviteDTO aktiviteToDTO(CrmAktivite a) {
        String cariAd = a.getCariHesapId() != null
                ? cariHesapRepository.findById(a.getCariHesapId()).map(c -> c.getAd()).orElse(null) : null;
        return CrmAktiviteDTO.builder()
                .id(a.getId()).sirketId(a.getSirketId()).tur(a.getTur()).baslik(a.getBaslik())
                .aciklama(a.getAciklama())
                .cariHesapId(a.getCariHesapId()).cariHesapAd(cariAd)
                .firsatId(a.getFirsatId()).leadId(a.getLeadId())
                .planlananTarih(a.getPlanlananTarih())
                .tamamlandi(a.getTamamlandi()).tamamlanmaTarihi(a.getTamamlanmaTarihi())
                .kullaniciId(a.getKullaniciId()).olusturmaTarihi(a.getOlusturmaTarihi())
                .build();
    }

    private CrmKampanyaDTO kampanyaToDTO(CrmKampanya k) {
        long leadSayisi = crmLeadRepository.countBySirketIdAndKampanyaId(k.getSirketId(), k.getId());
        return CrmKampanyaDTO.builder()
                .id(k.getId()).sirketId(k.getSirketId()).ad(k.getAd()).tur(k.getTur())
                .durum(k.getDurum()).baslangic(k.getBaslangic()).bitis(k.getBitis())
                .butce(k.getButce()).harcama(k.getHarcama()).hedefKisi(k.getHedefKisi())
                .aciklama(k.getAciklama()).leadSayisi(leadSayisi)
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .build();
    }
}
