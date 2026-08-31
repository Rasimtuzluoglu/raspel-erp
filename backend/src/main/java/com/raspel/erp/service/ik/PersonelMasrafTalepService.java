package com.raspel.erp.service.ik;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.dto.ik.PersonelMasrafTalepDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.PersonelMasrafTalep;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.service.finans.MasrafService;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.OnayAyariService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PersonelMasrafTalepService {

    private final PersonelMasrafTalepRepository talepRepository;
    private final PersonelRepository personelRepository;
    private final KullaniciRepository kullaniciRepository;
    private final MasrafService masrafService;
    private final BildirimService bildirimService;
    private final TenantChecker tenantChecker;
    private final OnayAyariService onayAyariService;

    @Transactional(readOnly = true)
    public Page<PersonelMasrafTalepDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return talepRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public List<PersonelMasrafTalepDTO> bekleyenleriGetir(Long sirketId) {
        return talepRepository.findBySirketIdAndDurumOrderByTarihDesc(sirketId, "BEKLEMEDE")
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonelMasrafTalepDTO> kullaniciTalepleri(Long kullaniciId) {
        return talepRepository.findByKullaniciIdOrderByTarihDesc(kullaniciId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonelMasrafTalepDTO> personelTalepleri(Long personelId) {
        return talepRepository.findByPersonelIdOrderByTarihDesc(personelId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    public PersonelMasrafTalepDTO talepOlustur(PersonelMasrafTalepDTO dto, Long sirketId, Long kullaniciId) {
        PersonelMasrafTalep talep = PersonelMasrafTalep.builder()
                .personelId(dto.getPersonelId())
                .kullaniciId(kullaniciId)
                .sirketId(sirketId)
                .tur(dto.getTur() != null ? dto.getTur() : "MASRAF")
                .kategori(dto.getKategori() != null ? dto.getKategori() : "DIGER")
                .tutar(dto.getTutar())
                .paraBirimi(dto.getParaBirimi() != null ? dto.getParaBirimi() : "TRY")
                .tarih(dto.getTarih() != null ? dto.getTarih() : LocalDate.now())
                .aciklama(dto.getAciklama())
                .belgeUrl(dto.getBelgeUrl())
                .durum("BEKLEMEDE")
                .build();

        talep = talepRepository.save(talep);

        // Otomatik onay kuralı: eşiğin altındaki masraf talepleri otomatik onaylanır.
        try {
            if (onayAyariService.otomatikOnayGecerli(sirketId, "MASRAF", talep.getTutar())) {
                talep.setDurum("ONAYLANDI");
                talep.setOnaylayan("OTOMATIK");
                talep.setOnayNotu("Otomatik onay (eşik altı tutar).");
                talep = talepRepository.save(talep);
                masrafIleEsle(talep);
                log.info("Masraf talebi otomatik onaylandı - Talep #{}", talep.getId());
            }
        } catch (Exception e) {
            log.warn("Otomatik onay kontrolü başarısız: {}", e.getMessage());
        }

        try {
            if (sirketId != null) {
                bildirimService.bildirimGonder(sirketId, "MASRAF_TALEBI",
                        "Yeni Personel Talebi: " + talep.getTur(),
                        "Tutar: " + talep.getTutar() + " ₺, Açıklama: " + talep.getAciklama());
            }
        } catch (Exception e) {
            log.warn("Talep bildirimi gönderilemedi: {}", e.getMessage());
        }

        return entityToDTO(talep);
    }

    /** Onaylanan MASRAF talebini finans masraf modülüne işler. */
    private void masrafIleEsle(PersonelMasrafTalep talep) {
        if (!"MASRAF".equalsIgnoreCase(talep.getTur())) return;
        String personelAd = "";
        if (talep.getPersonelId() != null) {
            personelAd = personelRepository.findById(talep.getPersonelId())
                    .map(p -> " (" + p.getAd() + " " + p.getSoyad() + ")")
                    .orElse("");
        }
        MasrafDTO masrafDTO = MasrafDTO.builder()
                .tarih(talep.getTarih())
                .tutar(talep.getTutar())
                .kategori("PERSONEL_" + talep.getKategori())
                .aciklama("Saha Masrafı" + personelAd + ": " + talep.getAciklama())
                .belgeNo("TALEP-" + talep.getId())
                .build();
        masrafService.olustur(masrafDTO, talep.getSirketId());
    }

    public PersonelMasrafTalepDTO onayla(Long id, String onaylayan, String onayNotu) {
        PersonelMasrafTalep talep = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel Masraf Talebi", id));
        tenantChecker.check(talep.getSirketId(), "Personel Masraf Talebi");

        talep.setDurum("ONAYLANDI");
        talep.setOnaylayan(onaylayan);
        talep.setOnayNotu(onayNotu);
        talep = talepRepository.save(talep);

        // Eğer MASRAF ise otomatik Finans Masraflar modülüne işle
        if ("MASRAF".equalsIgnoreCase(talep.getTur())) {
            try {
                masrafIleEsle(talep);
            } catch (Exception e) {
                throw new BusinessException("Onaylanan masraf finans modülüne işlenemedi: " + e.getMessage());
            }
        }

        return entityToDTO(talep);
    }

    public PersonelMasrafTalepDTO reddet(Long id, String onaylayan, String onayNotu) {
        PersonelMasrafTalep talep = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel Masraf Talebi", id));
        tenantChecker.check(talep.getSirketId(), "Personel Masraf Talebi");

        talep.setDurum("REDDEDILDI");
        talep.setOnaylayan(onaylayan);
        talep.setOnayNotu(onayNotu);
        return entityToDTO(talepRepository.save(talep));
    }

    public void sil(Long id) {
        PersonelMasrafTalep talep = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel Masraf Talebi", id));
        tenantChecker.check(talep.getSirketId(), "Personel Masraf Talebi");
        talepRepository.delete(talep);
    }

    private PersonelMasrafTalepDTO entityToDTO(PersonelMasrafTalep t) {
        String personelAd = null;
        String departman = null;
        if (t.getPersonelId() != null) {
            Optional<Personel> pOpt = personelRepository.findById(t.getPersonelId());
            if (pOpt.isPresent()) {
                Personel p = pOpt.get();
                personelAd = p.getAd() + " " + p.getSoyad();
                departman = p.getDepartman();
            }
        }

        String kullaniciAd = null;
        if (t.getKullaniciId() != null) {
            Optional<Kullanici> kOpt = kullaniciRepository.findById(t.getKullaniciId());
            if (kOpt.isPresent()) {
                Kullanici k = kOpt.get();
                kullaniciAd = (k.getDisplayName() != null && !k.getDisplayName().isBlank()) ? k.getDisplayName() : k.getUsername();
            }
        }

        return PersonelMasrafTalepDTO.builder()
                .id(t.getId())
                .personelId(t.getPersonelId())
                .personelAdi(personelAd != null ? personelAd : kullaniciAd)
                .departman(departman)
                .kullaniciId(t.getKullaniciId())
                .kullaniciAdi(kullaniciAd)
                .sirketId(t.getSirketId())
                .tur(t.getTur())
                .kategori(t.getKategori())
                .tutar(t.getTutar())
                .paraBirimi(t.getParaBirimi())
                .tarih(t.getTarih())
                .aciklama(t.getAciklama())
                .belgeUrl(t.getBelgeUrl())
                .durum(t.getDurum())
                .onaylayan(t.getOnaylayan())
                .onayNotu(t.getOnayNotu())
                .olusturmaTarihi(t.getOlusturmaTarihi())
                .build();
    }
}
