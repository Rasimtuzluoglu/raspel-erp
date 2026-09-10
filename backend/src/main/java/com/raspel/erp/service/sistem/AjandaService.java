package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AjandaGorevDTO;
import com.raspel.erp.dto.sistem.AjandaHatirlaticiDTO;
import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.entity.sistem.AjandaGorev;
import com.raspel.erp.entity.sistem.AjandaHatirlatici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.AjandaGorevRepository;
import com.raspel.erp.repository.sistem.AjandaHatirlaticiRepository;
import com.raspel.erp.repository.sistem.GorevRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AjandaService {

    private final GorevRepository gorevRepository;
    private final FaturaRepository faturaRepository;
    private final AjandaGorevRepository ajandaGorevRepository;
    private final AjandaHatirlaticiRepository ajandaHatirlaticiRepository;

    @Transactional(readOnly = true)
    public List<AjandaOlayDTO> olaylar(Long sirketId, LocalDate baslangic, LocalDate bitis, Long kullaniciId) {
        List<AjandaOlayDTO> olaylar = new ArrayList<>();

        List<Gorev> gorevler = gorevRepository.sirketGorevleri(sirketId, baslangic, bitis);
        for (Gorev g : gorevler) {
            LocalDate tarih = g.getBaslangic() != null ? g.getBaslangic() : baslangic;
            olaylar.add(AjandaOlayDTO.builder()
                    .tarih(tarih).tip("GOREV")
                    .baslik(g.getAd())
                    .aciklama("Durum: " + g.getDurum() + (g.getAtanan() != null ? " · Atanan: " + g.getAtanan() : ""))
                    .build());
        }

        List<Fatura> vadeler = faturaRepository.findVadesiYaklasan(
                sirketId, Fatura.FaturaDurum.KESILDI, List.of("ODENDI", "IPTAL"), baslangic, bitis);
        for (Fatura f : vadeler) {
            if (f.getVadeTarihi() == null) continue;
            olaylar.add(AjandaOlayDTO.builder()
                    .tarih(f.getVadeTarihi()).tip("VADE")
                    .baslik("Fatura #" + f.getFaturaNumarasi())
                    .aciklama((f.getCariHesap() != null ? f.getCariHesap().getAd() + " · " : "")
                            + "Kalan: " + (f.getKalanTutar() != null ? f.getKalanTutar() : "0"))
                    .build());
        }

        // Kişisel görevler
        if (kullaniciId != null) {
            List<AjandaGorev> gorevlerim = ajandaGorevRepository
                    .findByKullaniciIdAndBitisTarihiBetweenOrderByBitisTarihiAsc(kullaniciId, baslangic, bitis);
            for (AjandaGorev g : gorevlerim) {
                if (g.getBitisTarihi() == null) continue;
                String aciklama = "Öncelik: " + oncelikAdi(g.getOncelik()) + " · " + (g.getDurum() != null ? g.getDurum() : "");
                if (g.getAciklama() != null && !g.getAciklama().isBlank()) {
                    aciklama += "\n" + g.getAciklama();
                }
                olaylar.add(AjandaOlayDTO.builder()
                        .tarih(g.getBitisTarihi()).tip("KISISEL_GOREV")
                        .baslik(g.getBaslik())
                        .aciklama(aciklama)
                        .build());
            }
            // Kişisel hatırlatıcılar
            List<AjandaHatirlatici> hatirlaticilar = ajandaHatirlaticiRepository
                    .findByKullaniciIdOrderByHatirlatmaZamaniAsc(kullaniciId);
            for (AjandaHatirlatici h : hatirlaticilar) {
                LocalDate tarih = h.getHatirlatmaZamani() != null ? h.getHatirlatmaZamani().toLocalDate() : null;
                if (tarih == null || tarih.isBefore(baslangic) || tarih.isAfter(bitis)) continue;
                olaylar.add(AjandaOlayDTO.builder()
                        .tarih(tarih).tip("HATIRLATICI")
                        .baslik(h.getBaslik())
                        .aciklama("Hatırlatıcı · " + (h.getHatirlatmaZamani() != null ? h.getHatirlatmaZamani().toLocalTime().toString().substring(0, 5) : ""))
                        .build());
            }
        }

        olaylar.sort(Comparator.comparing(AjandaOlayDTO::getTarih));
        return olaylar;
    }

    private String oncelikAdi(String oncelik) {
        if (oncelik == null) return "Orta";
        return switch (oncelik) {
            case "YUKSEK" -> "Yüksek";
            case "DUSUK" -> "Düşük";
            default -> "Orta";
        };
    }

    // ---------- Kişisel Görevler ----------

    @Transactional(readOnly = true)
    public List<AjandaGorevDTO> gorevler(Long kullaniciId) {
        return ajandaGorevRepository.findByKullaniciIdOrderByBitisTarihiAsc(kullaniciId)
                .stream().map(this::gorevToDTO).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public AjandaGorevDTO gorevOlustur(AjandaGorevDTO dto, Long kullaniciId, Long sirketId) {
        if (dto.getBaslik() == null || dto.getBaslik().isBlank()) {
            throw new BusinessException("Görev başlığı zorunludur");
        }
        AjandaGorev g = AjandaGorev.builder()
                .kullaniciId(kullaniciId)
                .sirketId(sirketId)
                .baslik(dto.getBaslik().trim())
                .aciklama(dto.getAciklama())
                .bitisTarihi(dto.getBitisTarihi())
                .oncelik(dto.getOncelik() != null ? dto.getOncelik() : "ORTA")
                .durum(dto.getDurum() != null ? dto.getDurum() : "BEKLIYOR")
                .build();
        return gorevToDTO(ajandaGorevRepository.save(g));
    }

    @Transactional
    public AjandaGorevDTO gorevGuncelle(Long id, AjandaGorevDTO dto, Long kullaniciId) {
        AjandaGorev g = ajandaGorevRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Görev", id));
        yetkiKontrol(g.getKullaniciId(), kullaniciId);
        if (dto.getBaslik() != null) g.setBaslik(dto.getBaslik().trim());
        if (dto.getAciklama() != null) g.setAciklama(dto.getAciklama());
        if (dto.getBitisTarihi() != null) g.setBitisTarihi(dto.getBitisTarihi());
        if (dto.getOncelik() != null) g.setOncelik(dto.getOncelik());
        if (dto.getDurum() != null) g.setDurum(dto.getDurum());
        return gorevToDTO(ajandaGorevRepository.save(g));
    }

    @Transactional
    public AjandaGorevDTO gorevTamamla(Long id, Long kullaniciId) {
        AjandaGorev g = ajandaGorevRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Görev", id));
        yetkiKontrol(g.getKullaniciId(), kullaniciId);
        g.setDurum("TAMAMLANDI");
        return gorevToDTO(ajandaGorevRepository.save(g));
    }

    @Transactional
    public void gorevSil(Long id, Long kullaniciId) {
        AjandaGorev g = ajandaGorevRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Görev", id));
        yetkiKontrol(g.getKullaniciId(), kullaniciId);
        ajandaGorevRepository.deleteById(id);
    }

    // ---------- Kişisel Hatırlatıcılar ----------

    @Transactional(readOnly = true)
    public List<AjandaHatirlaticiDTO> hatirlaticilar(Long kullaniciId) {
        return ajandaHatirlaticiRepository.findByKullaniciIdOrderByHatirlatmaZamaniAsc(kullaniciId)
                .stream().map(this::hatirlaticiToDTO).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public AjandaHatirlaticiDTO hatirlaticiOlustur(AjandaHatirlaticiDTO dto, Long kullaniciId, Long sirketId) {
        if (dto.getBaslik() == null || dto.getBaslik().isBlank()) {
            throw new BusinessException("Hatırlatıcı başlığı zorunludur");
        }
        if (dto.getHatirlatmaZamani() == null) {
            throw new BusinessException("Hatırlatma zamanı zorunludur");
        }
        AjandaHatirlatici h = AjandaHatirlatici.builder()
                .kullaniciId(kullaniciId)
                .sirketId(sirketId)
                .gorevId(dto.getGorevId())
                .baslik(dto.getBaslik().trim())
                .hatirlatmaZamani(dto.getHatirlatmaZamani())
                .bildirildi(false)
                .build();
        return hatirlaticiToDTO(ajandaHatirlaticiRepository.save(h));
    }

    @Transactional
    public void hatirlaticiSil(Long id, Long kullaniciId) {
        AjandaHatirlatici h = ajandaHatirlaticiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hatırlatıcı", id));
        yetkiKontrol(h.getKullaniciId(), kullaniciId);
        ajandaHatirlaticiRepository.deleteById(id);
    }

    /** Vadesi gelmiş, bildirilmemiş hatırlatıcıları döndürür ve bildirildi işaretler. */
    @Transactional
    public List<AjandaHatirlaticiDTO> vadesiGelenHatirlaticilar(java.time.LocalDateTime simdi) {
        List<AjandaHatirlatici> gelen = ajandaHatirlaticiRepository
                .findByBildirildiFalseAndHatirlatmaZamaniLessThanEqual(simdi);
        for (AjandaHatirlatici h : gelen) {
            h.setBildirildi(true);
            ajandaHatirlaticiRepository.save(h);
        }
        return gelen.stream().map(this::hatirlaticiToDTO).collect(java.util.stream.Collectors.toList());
    }

    private void yetkiKontrol(Long sahipKullaniciId, Long istekKullaniciId) {
        if (sahipKullaniciId == null || !sahipKullaniciId.equals(istekKullaniciId)) {
            throw new BusinessException("Bu kayda erişim yetkiniz bulunmuyor.");
        }
    }

    private AjandaGorevDTO gorevToDTO(AjandaGorev g) {
        return AjandaGorevDTO.builder()
                .id(g.getId()).baslik(g.getBaslik()).aciklama(g.getAciklama())
                .bitisTarihi(g.getBitisTarihi()).oncelik(g.getOncelik()).durum(g.getDurum())
                .olusturmaTarihi(g.getOlusturmaTarihi()).build();
    }

    private AjandaHatirlaticiDTO hatirlaticiToDTO(AjandaHatirlatici h) {
        return AjandaHatirlaticiDTO.builder()
                .id(h.getId()).gorevId(h.getGorevId()).baslik(h.getBaslik())
                .hatirlatmaZamani(h.getHatirlatmaZamani()).bildirildi(h.getBildirildi())
                .olusturmaTarihi(h.getOlusturmaTarihi()).build();
    }
}
