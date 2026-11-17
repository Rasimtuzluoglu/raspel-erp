package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SahaOzetDTO;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.NotRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Saha portalı "Bugün" özeti. Tüm sayımlar oturum kullanıcısına göre
 * daraltılır; şirket geneli veri dönmez (sürücü izolasyonu).
 */
@Service
@RequiredArgsConstructor
public class SahaOzetService {

    private static final List<String> KAPALI_DURUMLAR = List.of("TESLIM_EDILDI", "IPTAL");

    private final TeslimatRepository teslimatRepository;
    private final NotRepository notRepository;
    private final PersonelIzinRepository personelIzinRepository;
    private final PersonelMasrafTalepRepository personelMasrafTalepRepository;
    private final PersonelRepository personelRepository;

    @Cacheable(cacheNames = "sahaOzet", key = "#sirketId + ':' + #kullaniciId")
    @Transactional(readOnly = true)
    public SahaOzetDTO ozet(Long sirketId, Long kullaniciId) {
        LocalDate bugun = LocalDate.now();
        if (sirketId == null || kullaniciId == null) {
            return bosOzet(bugun);
        }

        LocalDateTime bas = bugun.atStartOfDay();
        LocalDateTime bit = bugun.plusDays(1).atStartOfDay();

        long bekleyen = teslimatRepository.countBySirketIdAndDriverIdAndDurum(
                sirketId, kullaniciId, "BEKLEMEDE");
        long yolda = teslimatRepository.countBySirketIdAndDriverIdAndDurum(
                sirketId, kullaniciId, "YOLDA");
        long bugunTeslim = teslimatRepository.countBySirketIdAndDriverIdAndTeslimTarihiBetweenAndDurum(
                sirketId, kullaniciId, bas, bit, "TESLIM_EDILDI");
        long geciken = teslimatRepository.countBySirketIdAndDriverIdAndBeklenenTeslimTarihiBeforeAndDurumNotIn(
                sirketId, kullaniciId, bugun, KAPALI_DURUMLAR);

        long bugunZiyaret = notRepository.countBySirketIdAndKullaniciIdAndOlusturmaTarihiBetween(
                sirketId, kullaniciId, bas, bit);
        Optional<Not> sonNot = notRepository
                .findTop1BySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(sirketId, kullaniciId);

        long bekleyenIzin = 0L;
        Long personelId = personelIdBul(kullaniciId);
        if (personelId != null) {
            bekleyenIzin = personelIzinRepository.countByPersonelIdAndDurum(personelId, "BEKLEMEDE");
        }
        long bekleyenMasraf = personelMasrafTalepRepository
                .countBySirketIdAndKullaniciIdAndDurum(sirketId, kullaniciId, "BEKLEMEDE");

        return SahaOzetDTO.builder()
                .tarih(bugun)
                .teslimat(SahaOzetDTO.Teslimat.builder()
                        .bekleyen(bekleyen)
                        .yolda(yolda)
                        .bugunTeslimEdilen(bugunTeslim)
                        .geciken(geciken)
                        .build())
                .ziyaret(SahaOzetDTO.Ziyaret.builder()
                        .bugun(bugunZiyaret)
                        .sonZiyaretTarihi(sonNot.map(Not::getOlusturmaTarihi).orElse(null))
                        .build())
                .talepler(SahaOzetDTO.Talepler.builder()
                        .bekleyenIzin(bekleyenIzin)
                        .bekleyenMasraf(bekleyenMasraf)
                        .build())
                .build();
    }

    /** Oturum kullanıcısının personel kaydı (yoksa null). Tenant dışına çıkmaz. */
    private Long personelIdBul(Long kullaniciId) {
        try {
            return personelRepository.findByKullaniciId(kullaniciId)
                    .map(com.raspel.erp.entity.ik.Personel::getId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private SahaOzetDTO bosOzet(LocalDate bugun) {        return SahaOzetDTO.builder()
                .tarih(bugun)
                .teslimat(SahaOzetDTO.Teslimat.builder().build())
                .ziyaret(SahaOzetDTO.Ziyaret.builder().build())
                .talepler(SahaOzetDTO.Talepler.builder().build())
                .build();
    }
}
