package com.raspel.erp.service.muhasebe;

import com.raspel.erp.dto.muhasebe.MuhasebeFisKalemDTO;
import com.raspel.erp.dto.muhasebe.MuhasebeFisiDTO;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.entity.muhasebe.HesapPlani;
import com.raspel.erp.repository.muhasebe.HesapPlaniRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Operasyonel belgelerden otomatik yevmiye fişi üretir.
 *
 * <p>Şu an kapsam: yalnızca <b>bordro</b>. Fatura/tahsilat entegrasyonu ayrı fazdadır.
 * Fiş üretimi iş akışını bloklamaz: hata durumunda loglanır, ilgili işlem devam eder.
 * Aynı kaynak için mükerrer fiş oluşturulmaz (kaynakTip + kaynakId).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtomatikMuhasebeService {

    public static final String KAYNAK_BORDRO = "BORDRO";

    private static final String HESAP_YONETIM_GIDER = "770";
    private static final String HESAP_PERSONELE_BORCLAR = "335";
    private static final String HESAP_ODENECEK_VERGI = "360";

    private final MuhasebeService muhasebeService;
    private final MuhasebeFisiRepository muhasebeFisiRepository;
    private final HesapPlaniRepository hesapPlaniRepository;

    /**
     * Bordro için yevmiye fişi: Borç 770 (brüt) / Alacak 335 (net) + Alacak 360 (kesinti).
     * Idempotenttir; aynı bordro için aktif fiş varsa tekrar oluşturmaz.
     */
    @Transactional
    public void bordroIsle(MaasBordro bordro) {
        if (bordro == null || bordro.getId() == null || bordro.getSirketId() == null) return;
        try {
            boolean mevcut = muhasebeFisiRepository
                    .findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(
                            bordro.getSirketId(), KAYNAK_BORDRO, bordro.getId(), "IPTAL")
                    .isPresent();
            if (mevcut) return;

            BigDecimal brut = nz(bordro.getBrutMaas());
            BigDecimal net = nz(bordro.getNetMaas());
            if (brut.signum() == 0 && net.signum() == 0) return;

            hesapGaranti(bordro.getSirketId(), HESAP_YONETIM_GIDER, "Genel Yönetim Giderleri", "GIDER");
            hesapGaranti(bordro.getSirketId(), HESAP_PERSONELE_BORCLAR, "Personele Borçlar", "PASIF");
            hesapGaranti(bordro.getSirketId(), HESAP_ODENECEK_VERGI, "Ödenecek Vergi ve Fonlar", "PASIF");

            String donem = bordro.getYil() + "-" + String.format("%02d", bordro.getAy() != null ? bordro.getAy() : 0);
            String personel = bordro.getPersonel() != null
                    ? bordro.getPersonel().getAd() + " " + bordro.getPersonel().getSoyad() : "";
            String aciklama = "Bordro " + donem + (personel.isBlank() ? "" : " - " + personel.trim());

            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            kalemler.add(MuhasebeFisKalemDTO.builder()
                    .hesapKodu(HESAP_YONETIM_GIDER).borc(brut).aciklama(aciklama).build());
            kalemler.add(MuhasebeFisKalemDTO.builder()
                    .hesapKodu(HESAP_PERSONELE_BORCLAR).alacak(net).aciklama(aciklama).build());

            BigDecimal fark = brut.subtract(net);
            if (fark.signum() >= 0) {
                kalemler.add(MuhasebeFisKalemDTO.builder()
                        .hesapKodu(HESAP_ODENECEK_VERGI).alacak(fark).aciklama(aciklama + " (kesintiler)").build());
            } else {
                kalemler.add(MuhasebeFisKalemDTO.builder()
                        .hesapKodu(HESAP_ODENECEK_VERGI).borc(fark.negate()).aciklama(aciklama + " (fazla ödeme)").build());
            }

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(bordro.getOdemeTarihi() != null ? bordro.getOdemeTarihi() : LocalDate.now())
                    .aciklama(aciklama)
                    .sirketId(bordro.getSirketId())
                    .kalemler(kalemler)
                    .kaynakTip(KAYNAK_BORDRO)
                    .kaynakId(bordro.getId())
                    .build());
            log.info("Bordro yevmiye fişi oluşturuldu - Bordro ID: {}", bordro.getId());
        } catch (Exception e) {
            log.warn("Bordro otomatik muhasebe fişi oluşturulamadı (bordro id: {}): {}",
                    bordro.getId(), e.getMessage());
        }
    }

    /** Bordro silindiğinde/güncellendiğinde bağlı aktif fişi iptal eder (audit için kaydı korur). */
    @Transactional
    public void bordroIptal(Long bordroId, Long sirketId) {
        if (bordroId == null || sirketId == null) return;
        try {
            muhasebeFisiRepository
                    .findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(sirketId, KAYNAK_BORDRO, bordroId, "IPTAL")
                    .ifPresent(f -> {
                        f.setDurum("IPTAL");
                        muhasebeFisiRepository.save(f);
                        log.info("Bordro yevmiye fişi iptal edildi - Fiş: {}, Bordro ID: {}", f.getFisNo(), bordroId);
                    });
        } catch (Exception e) {
            log.warn("Bordro yevmiye fişi iptal edilemedi (bordro id: {}): {}", bordroId, e.getMessage());
        }
    }

    private void hesapGaranti(Long sirketId, String kod, String ad, String tip) {
        if (hesapPlaniRepository.findBySirketIdAndKod(sirketId, kod).isPresent()) return;
        hesapPlaniRepository.save(HesapPlani.builder()
                .kod(kod).ad(ad).tip(tip)
                .sirketId(sirketId).aktif(true)
                .build());
        log.info("Hesap planına otomatik hesap eklendi - Şirket: {}, Kod: {}", sirketId, kod);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
