package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.dto.sistem.SohbetOdaDTO;
import com.raspel.erp.dto.sistem.SohbetOdaUyeDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.entity.sistem.SohbetOda;
import com.raspel.erp.entity.sistem.SohbetOdaUye;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import com.raspel.erp.repository.sistem.SohbetOdaRepository;
import com.raspel.erp.repository.sistem.SohbetOdaUyeRepository;
import com.raspel.erp.util.SohbetDosyaUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SohbetOdaService {

    private final SohbetOdaRepository odaRepository;
    private final SohbetOdaUyeRepository uyeRepository;
    private final SohbetMesajRepository mesajRepository;
    private final KullaniciRepository kullaniciRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DosyaDepolamaService dosyaDepolama;
    private final SohbetHizSiniri hizSiniri;

    private static final String DOSYA_KLASOR = "sohbet";

    /**
     * Şirketin görünür odaları.
     *
     * Faz 8: (a) özel kanallar yalnız üyelerine görünür, (b) üye listesi ve
     * kullanıcı adları TOPLU çekilir. Önce her oda için ayrı üye sorgusu ve her
     * üye için ayrı kullanıcı sorgusu çalışıyordu (N+1); sidebar her açılışta
     * onlarca sorgu üretiyordu.
     */
    @Transactional(readOnly = true)
    public List<SohbetOdaDTO> odalar(Long sirketId, Long kullaniciId) {
        List<SohbetOda> odalar = odaRepository.findBySirketIdOrderByAd(sirketId);
        List<SohbetOdaUye> uyelikler = kullaniciId != null
                ? uyeRepository.findByKullaniciId(kullaniciId)
                : List.of();
        Map<Long, SohbetOdaUye> uyelikMap = new HashMap<>();
        for (SohbetOdaUye u : uyelikler) uyelikMap.put(u.getOdaId(), u);

        List<SohbetOda> gorunen = odalar.stream()
                .filter(o -> !o.isOzel() || uyelikMap.containsKey(o.getId()))
                .collect(Collectors.toList());
        if (gorunen.isEmpty()) return List.of();

        List<Long> odaIdleri = gorunen.stream().map(SohbetOda::getId).collect(Collectors.toList());
        Map<Long, List<SohbetOdaUye>> uyeMap = uyeRepository.findByOdaIdIn(odaIdleri).stream()
                .collect(Collectors.groupingBy(SohbetOdaUye::getOdaId));
        Set<Long> kullaniciIdleri = new HashSet<>();
        uyeMap.values().forEach(list -> list.forEach(u -> kullaniciIdleri.add(u.getKullaniciId())));
        Map<Long, String> adlar = kullaniciAdlari(kullaniciIdleri);

        return gorunen.stream()
                .map(o -> toDTO(o, uyelikMap.get(o.getId()), uyeMap.getOrDefault(o.getId(), List.of()), adlar))
                .collect(Collectors.toList());
    }

    @Transactional
    public SohbetOdaDTO olustur(SohbetOdaDTO dto, Long sirketId, Long kullaniciId) {
        if (dto.getAd() == null || dto.getAd().isBlank()) {
            throw new BusinessException("Oda adı boş olamaz");
        }
        SohbetOda oda = SohbetOda.builder()
                .sirketId(sirketId)
                .ad(dto.getAd().trim())
                .aciklama(dto.getAciklama())
                .ozel(dto.isOzel())
                .olusturanKullaniciId(kullaniciId)
                .build();
        oda = odaRepository.save(oda);
        // Faz 3.2: kurucu OWNER rolüyle üye olur.
        uyeRepository.save(SohbetOdaUye.builder().odaId(oda.getId()).kullaniciId(kullaniciId).rol("OWNER").build());
        // Faz 8: oda oluştururken seçilen üyeler doğrudan eklenir; önceden kanal
        // yalnız kurucuyla açılıyor, kimse fark etmiyordu.
        Set<Long> eklenecek = dto.getUyeIdleri() != null ? new LinkedHashSet<>(dto.getUyeIdleri()) : Set.of();
        for (Long hedef : eklenecek) {
            if (hedef == null || hedef.equals(kullaniciId)) continue;
            Kullanici k = kullaniciRepository.findById(hedef).orElse(null);
            if (k == null) continue;
            if (sirketId != null && k.getSirketId() != null && !sirketId.equals(k.getSirketId())) continue;
            if (!uyeRepository.existsByOdaIdAndKullaniciId(oda.getId(), hedef)) {
                uyeRepository.save(SohbetOdaUye.builder()
                        .odaId(oda.getId()).kullaniciId(hedef).rol("MEMBER").build());
            }
        }
        return dtoUret(oda, kullaniciId);
    }

    @Transactional
    public SohbetOdaDTO guncelle(Long odaId, SohbetOdaDTO dto, Long sirketId, Long kullaniciId) {
        SohbetOda oda = yetkiliOdaBul(odaId, sirketId, kullaniciId);
        if (dto.getAd() != null && !dto.getAd().isBlank()) {
            oda.setAd(dto.getAd().trim());
        }
        if (dto.getAciklama() != null) {
            oda.setAciklama(dto.getAciklama());
        }
        oda = odaRepository.save(oda);
        return dtoUret(oda, kullaniciId);
    }

    @Transactional
    public void sil(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = yetkiliOdaBul(odaId, sirketId, kullaniciId);
        // Faz 8: mesajlar da silinir; önceden oda silinse bile satırlar öksüz kalıyordu.
        mesajRepository.deleteByOdaId(odaId);
        uyeRepository.deleteByOdaId(odaId);
        odaRepository.delete(oda);
        odaSilindiYayinla(oda.getSirketId(), odaId);
    }

    @Transactional
    public SohbetOdaDTO uyeEkle(Long odaId, Long hedefKullaniciId, Long sirketId, Long istekSahibiId) {
        SohbetOda oda = uyeOdasiBul(odaId, sirketId);
        // Faz 3.2: üye ekleme yalnızca OWNER/ADMIN (veya global ADMIN).
        if (!odaYoneticisiMi(oda, odaId, istekSahibiId)) {
            throw new BusinessException("Üye eklemek için oda yöneticisi olmalısınız");
        }
        if (hedefKullaniciId == null) {
            throw new BusinessException("Eklenecek kullanıcı belirtilmedi");
        }
        if (!uyeRepository.existsByOdaIdAndKullaniciId(odaId, hedefKullaniciId)) {
            uyeRepository.save(SohbetOdaUye.builder().odaId(odaId).kullaniciId(hedefKullaniciId).rol("MEMBER").build());
        }
        return toDTO(oda, istekSahibiId, true);
    }

    @Transactional
    public SohbetOdaDTO uyeCikar(Long odaId, Long hedefKullaniciId, Long sirketId, Long istekSahibiId) {
        SohbetOda oda = yetkiliOdaBul(odaId, sirketId, istekSahibiId);
        uyeRepository.deleteByOdaIdAndKullaniciId(odaId, hedefKullaniciId);
        return toDTO(oda, istekSahibiId, uyeRepository.existsByOdaIdAndKullaniciId(odaId, istekSahibiId));
    }

    @Transactional
    public SohbetOdaDTO katil(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = odaBul(odaId, sirketId);
        // Faz 8: özel kanala kendi kendine katılma kapalı.
        if (oda.isOzel()) {
            throw new BusinessException("Bu kanal özel; yönetici sizi eklemeli");
        }
        if (!uyeRepository.existsByOdaIdAndKullaniciId(odaId, kullaniciId)) {
            uyeRepository.save(SohbetOdaUye.builder().odaId(odaId).kullaniciId(kullaniciId).build());
        }
        return toDTO(oda, kullaniciId, true);
    }

    /**
     * Odadan ayrılma.
     *
     * Faz 8: önceden kurucu ayrılınca oda ve TÜM üyelikler siliniyordu; tek
     * tıklama kanalı yok ediyordu. Artık yönetim en eski üyeye devredilir;
     * son üye ayrılırsa oda ve mesajları temizlenir.
     */
    @Transactional
    public void ayril(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = odaBul(odaId, sirketId);
        List<SohbetOdaUye> uyeler = uyeRepository.findByOdaId(odaId);
        SohbetOdaUye ayrilan = uyeler.stream()
                .filter(u -> Objects.equals(u.getKullaniciId(), kullaniciId))
                .findFirst()
                .orElse(null);
        boolean yonetici = (oda.getOlusturanKullaniciId() != null && oda.getOlusturanKullaniciId().equals(kullaniciId))
                || (ayrilan != null && "OWNER".equalsIgnoreCase(ayrilan.getRol()));

        if (yonetici) {
            List<SohbetOdaUye> digerleri = uyeler.stream()
                    .filter(u -> !Objects.equals(u.getKullaniciId(), kullaniciId))
                    .sorted(Comparator.comparing(SohbetOdaUye::getEklenmeTarihi,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            if (digerleri.isEmpty()) {
                mesajRepository.deleteByOdaId(odaId);
                uyeRepository.deleteByOdaId(odaId);
                odaRepository.delete(oda);
                odaSilindiYayinla(oda.getSirketId(), odaId);
                return;
            }
            SohbetOdaUye yeniSahip = digerleri.get(0);
            yeniSahip.setRol("OWNER");
            uyeRepository.save(yeniSahip);
            oda.setOlusturanKullaniciId(yeniSahip.getKullaniciId());
            odaRepository.save(oda);
        }
        uyeRepository.deleteByOdaIdAndKullaniciId(odaId, kullaniciId);
    }

    @Transactional(readOnly = true)
    public List<SohbetMesajDTO> mesajlar(Long odaId, Long sirketId, Long kullaniciId) {
        return mesajlar(odaId, sirketId, kullaniciId, null, null);
    }

    /**
     * Faz 3.3: oda mesajları cursor (id) tabanlı sayfalanır. {@code cursor=null}
     * ise en yeni {@code limit} mesaj; aksi halde cursor'dan eski mesajlar.
     * Dönen liste ekranda eskiden yeniye olacak şekilde artan sırada verilir.
     */
    @Transactional(readOnly = true)
    public List<SohbetMesajDTO> mesajlar(Long odaId, Long sirketId, Long kullaniciId,
                                         Long cursor, Integer limit) {
        uyeKontrol(odaId, sirketId, kullaniciId);
        int boyut = (limit == null || limit <= 0) ? 50 : Math.min(limit, 100);
        org.springframework.data.domain.Pageable sayfa = org.springframework.data.domain.PageRequest.of(0, boyut);
        List<SohbetMesaj> mesajlar = (cursor == null)
                ? mesajRepository.findByOdaIdOrderByIdDesc(odaId, sayfa)
                : mesajRepository.findByOdaIdAndIdLessThanOrderByIdDesc(odaId, cursor, sayfa);
        java.util.Collections.reverse(mesajlar);
        return mesajlar.stream().map(this::mesajDTO).collect(Collectors.toList());
    }

    @Transactional
    public SohbetMesajDTO mesajGonder(Long odaId, SohbetMesajDTO dto, Long sirketId, Long kullaniciId, String kullaniciAd) {
        uyeKontrol(odaId, sirketId, kullaniciId);
        boolean mesajVar = dto.getMesaj() != null && !dto.getMesaj().isBlank();
        boolean dosyaVar = dto.getDosyaUrl() != null && !dto.getDosyaUrl().isBlank();
        if (!mesajVar && !dosyaVar) {
            throw new BusinessException("Mesaj boş olamaz");
        }
        // Faz 8: yalnızca kendi yükleme yolumuz; dış URL/tracking engellenir.
        SohbetDosyaUrl.dogrula(dto.getDosyaUrl());
        hizSiniri.kontrolEt("sohbet-mesaj:" + kullaniciId, 20, 10_000,
                "Çok hızlı mesaj gönderiyorsunuz, lütfen biraz bekleyin.");
        SohbetMesaj mesaj = SohbetMesaj.builder()
                .sirketId(sirketId)
                .odaId(odaId)
                .kullaniciId(kullaniciId)
                .kullaniciAd(kullaniciAd)
                .mesaj(mesajVar ? dto.getMesaj().trim() : "")
                .dosyaUrl(dosyaVar ? dto.getDosyaUrl() : null)
                .build();
        mesaj = mesajRepository.save(mesaj);
        SohbetMesajDTO dtoKayit = mesajDTO(mesaj);
        try {
            messagingTemplate.convertAndSend("/topic/sohbet/oda/" + sirketId + "/" + odaId, dtoKayit);
            // Faz 8: seçili olmayan odalarda da okunmamış rozeti canlansın.
            messagingTemplate.convertAndSend("/topic/sohbet/" + sirketId + "/rozet",
                    Map.of("odaId", odaId));
        } catch (Exception e) {
            log.warn("Oda mesajı yayınlanamadı: {}", e.getMessage());
        }
        return dtoKayit;
    }

    /**
     * Oda mesajını siler. Yalnızca ADMIN rolü erişebilir (controller'da @PreAuthorize
     * ile kısıtlanır). Mesajın odaya ve şirkete ait olduğu doğrulanır.
     */
    @Transactional
    public void mesajSil(Long odaId, Long mesajId, Long sirketId) {
        odaBul(odaId, sirketId);
        SohbetMesaj mesaj = mesajRepository.findById(mesajId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesaj bulunamadı"));
        if (mesaj.getOdaId() == null || !mesaj.getOdaId().equals(odaId)) {
            throw new BusinessException("Mesaj bu odaya ait değil");
        }
        if (sirketId != null && !sirketId.equals(mesaj.getSirketId())) {
            throw new BusinessException("Bu mesaja erişim yetkiniz yok");
        }
        mesajRepository.delete(mesaj);
        try {
            if (sirketId != null) {
                messagingTemplate.convertAndSend("/topic/sohbet/oda/" + sirketId + "/" + odaId + "/sil", mesajId);
            }
        } catch (Exception e) {
            log.warn("Oda mesaj silme olayı yayınlanamadı: {}", e.getMessage());
        }
    }

    @Transactional
    public void okunduIsaretle(Long odaId, Long kullaniciId) {
        uyeRepository.findByOdaIdAndKullaniciId(odaId, kullaniciId).ifPresent(u -> {
            u.setSonOkuma(Instant.now());
            uyeRepository.save(u);
        });
    }

    @Transactional
    public String dosyaYukle(Long odaId, MultipartFile file, Long sirketId, Long kullaniciId) {
        uyeKontrol(odaId, sirketId, kullaniciId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya boş");
        }
        // Dosya yükleme de kötüye kullanılabilir: dakikada en fazla 10 dosya.
        hizSiniri.kontrolEt("sohbet-dosya:" + kullaniciId, 10, 60_000,
                "Çok sık dosya yüklediniz, lütfen biraz bekleyin.");
        // Dosya tipi whitelist'i: script/executable/svg/html gibi riskli turler reddedilir.
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!IZINLI_SOHBET_UZANTILARI.contains(ext)) {
            throw new BusinessException("Bu dosya tipi desteklenmiyor: " + (ext.isBlank() ? "(uzantısız)" : ext));
        }
        // Resim uzantilari icin icerik imzasi dogrulanir (stored XSS/polyglot engeli).
        boolean resim = java.util.Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext);
        try {
            // Tenant klasörü: sohbet dosyaları düz `sohbet/` altında tutuluyordu ve
            // indirme ucu önce `sohbet/s{id}` sonra düz `sohbet` klasörünü deniyordu.
            // Dosyalar düz klasörde olduğu için her istek oraya düşüyor, UUID'yi bilen
            // herhangi bir kullanıcı BAŞKA ŞİRKETİN sohbet dosyasını indirebiliyordu.
            String klasor = sirketId != null ? DOSYA_KLASOR + "/s" + sirketId : DOSYA_KLASOR;
            String filename = resim
                    ? dosyaDepolama.kaydetResimDogrulamali(klasor, file)
                    : dosyaDepolama.kaydet(klasor, file);
            return "/api/uploads/sohbet/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Dosya yüklenemedi");
        }
    }

    private static final java.util.Set<String> IZINLI_SOHBET_UZANTILARI = java.util.Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".webp", ".gif", ".txt", ".csv",
            ".doc", ".docx", ".xls", ".xlsx", ".zip");

    private SohbetOda odaBul(Long odaId, Long sirketId) {
        SohbetOda oda = odaRepository.findById(odaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oda bulunamadı"));
        if (sirketId != null && !sirketId.equals(oda.getSirketId())) {
            throw new BusinessException("Bu odaya erişim yetkiniz yok");
        }
        return oda;
    }

    private SohbetOda uyeOdasiBul(Long odaId, Long sirketId) {
        return odaBul(odaId, sirketId);
    }

    private SohbetOda yetkiliOdaBul(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = odaBul(odaId, sirketId);
        if (!odaYoneticisiMi(oda, odaId, kullaniciId)) {
            throw new BusinessException("Bu odayı yönetme yetkiniz yok");
        }
        return oda;
    }

    /**
     * Faz 3.2: kullanıcı global ADMIN, oda kurucusu veya oda içi OWNER/ADMIN
     * üyelik rolüne sahipse odanın yöneticisidir.
     */
    private boolean odaYoneticisiMi(SohbetOda oda, Long odaId, Long kullaniciId) {
        if (kullaniciId == null) return false;
        boolean globalAdmin = kullaniciRepository.findById(kullaniciId)
                .map(k -> "ADMIN".equalsIgnoreCase(k.getRole()))
                .orElse(false);
        if (globalAdmin) return true;
        if (oda.getOlusturanKullaniciId() != null && oda.getOlusturanKullaniciId().equals(kullaniciId)) {
            return true;
        }
        return uyeRepository.findByOdaIdAndKullaniciId(odaId, kullaniciId)
                .map(u -> "OWNER".equalsIgnoreCase(u.getRol()) || "ADMIN".equalsIgnoreCase(u.getRol()))
                .orElse(false);
    }

    private void uyeKontrol(Long odaId, Long sirketId, Long kullaniciId) {
        odaBul(odaId, sirketId);
        boolean uye = uyeRepository.existsByOdaIdAndKullaniciId(odaId, kullaniciId);
        boolean yonetici = kullaniciId != null && kullaniciRepository.findById(kullaniciId)
                .map(k -> "ADMIN".equalsIgnoreCase(k.getRole()))
                .orElse(false);
        if (!uye && !yonetici) {
            throw new BusinessException("Mesaj görmek için önce odaya katılın");
        }
    }

    private void odaSilindiYayinla(Long sirketId, Long odaId) {
        try {
            if (sirketId != null) {
                messagingTemplate.convertAndSend("/topic/sohbet/" + sirketId + "/oda-sil", odaId);
            }
        } catch (Exception e) {
            log.warn("Oda silme olayı yayınlanamadı: {}", e.getMessage());
        }
    }

    private Map<Long, String> kullaniciAdlari(Collection<Long> idler) {
        if (idler == null || idler.isEmpty()) return Map.of();
        Map<Long, String> adlar = new HashMap<>();
        for (Kullanici k : kullaniciRepository.findAllById(idler)) {
            adlar.put(k.getId(), k.getDisplayName());
        }
        return adlar;
    }

    private SohbetOdaDTO dtoUret(SohbetOda oda, Long kullaniciId) {
        List<SohbetOdaUye> uyeler = uyeRepository.findByOdaId(oda.getId());
        Set<Long> idler = uyeler.stream().map(SohbetOdaUye::getKullaniciId).collect(Collectors.toSet());
        return toDTO(oda, uyeRepository.findByOdaIdAndKullaniciId(oda.getId(), kullaniciId).orElse(null),
                uyeler, kullaniciAdlari(idler));
    }

    private SohbetOdaDTO toDTO(SohbetOda oda, Long kullaniciId, boolean uyeMi) {
        List<SohbetOdaUye> uyeler = uyeRepository.findByOdaId(oda.getId());
        Set<Long> idler = uyeler.stream().map(SohbetOdaUye::getKullaniciId).collect(Collectors.toSet());
        return toDTO(oda, uyeRepository.findByOdaIdAndKullaniciId(oda.getId(), kullaniciId).orElse(null),
                uyeler, kullaniciAdlari(idler));
    }

    /**
     * Toplu verilmiş üye/kullanıcı adlarıyla DTO üretir (N+1 yok).
     */
    private SohbetOdaDTO toDTO(SohbetOda oda, SohbetOdaUye uyelik,
                               List<SohbetOdaUye> uyeler, Map<Long, String> adlar) {
        boolean uyeMi = uyelik != null;
        List<SohbetOdaUyeDTO> uyeDTOleri = new ArrayList<>();
        for (SohbetOdaUye u : uyeler) {
            uyeDTOleri.add(SohbetOdaUyeDTO.builder()
                    .kullaniciId(u.getKullaniciId())
                    .kullaniciAd(adlar.get(u.getKullaniciId()))
                    .rol(u.getRol())
                    .build());
        }
        long okunmamis = 0;
        if (uyeMi) {
            Instant esik = uyelik.getSonOkuma() != null ? uyelik.getSonOkuma() : Instant.EPOCH;
            okunmamis = mesajRepository.countByOdaIdAndOlusturmaTarihiGreaterThan(oda.getId(), esik);
        }
        return SohbetOdaDTO.builder()
                .id(oda.getId())
                .sirketId(oda.getSirketId())
                .ad(oda.getAd())
                .aciklama(oda.getAciklama())
                .olusturanKullaniciId(oda.getOlusturanKullaniciId())
                .ozel(oda.isOzel())
                .uyeMi(uyeMi)
                .uyeSayisi(uyeDTOleri.size())
                .okunmamisSayisi(okunmamis)
                .uyeler(uyeDTOleri)
                .build();
    }

    private SohbetMesajDTO mesajDTO(SohbetMesaj m) {
        return SohbetMesajDTO.builder()
                .id(m.getId())
                .sirketId(m.getSirketId())
                .odaId(m.getOdaId())
                .kullaniciId(m.getKullaniciId())
                .kullaniciAd(m.getKullaniciAd())
                .mesaj(m.getMesaj())
                .dosyaUrl(m.getDosyaUrl())
                .olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
