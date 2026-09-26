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
 * <p>Kapsam: bordro, satış faturası, alış faturası ve tahsilat.
 * Fiş üretimi iş akışını bloklamaz: hata durumunda loglanır, ilgili işlem devam eder.
 * Aynı kaynak için mükerrer fiş oluşturulmaz (kaynakTip + kaynakId).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtomatikMuhasebeService {

    public static final String KAYNAK_BORDRO = "BORDRO";
    public static final String KAYNAK_SATIS_FATURA = "SATIS_FATURA";
    public static final String KAYNAK_ALIS_FATURA = "ALIS_FATURA";
    public static final String KAYNAK_TAHSILAT = "TAHSILAT";

    private static final String HESAP_YONETIM_GIDER = "770";
    private static final String HESAP_PERSONELE_BORCLAR = "335";
    private static final String HESAP_ODENECEK_VERGI = "360";
    // Ticari standart hesap eşlemesi
    private static final String HESAP_KASA = "100";
    private static final String HESAP_BANKA = "102";
    private static final String HESAP_ALICILAR = "120";
    private static final String HESAP_TICARI_MALLAR = "153";
    private static final String HESAP_INDIRILECEK_KDV = "191";
    private static final String HESAP_SATICILAR = "320";
    private static final String HESAP_HESAPLANAN_KDV = "391";
    private static final String HESAP_YURTICI_SATISLAR = "600";

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

    /**
     * Satış faturası yevmiye fişi: Borç 120 (Alıcılar, KDV dahil) / Alacak 600 (matrah) + Alacak 391 (KDV).
     */
    @Transactional
    public void satisFaturaIsle(com.raspel.erp.entity.ticaret.Fatura fatura) {
        faturaIsle(fatura, true);
    }

    /** Alış faturası yevmiye fişi: Borç 153 (matrah) + Borç 191 (KDV) / Alacak 320 (satıcılar). */
    @Transactional
    public void alisFaturaIsle(com.raspel.erp.entity.ticaret.Fatura fatura) {
        faturaIsle(fatura, false);
    }

    private void faturaIsle(com.raspel.erp.entity.ticaret.Fatura fatura, boolean satis) {
        if (fatura == null || fatura.getId() == null || fatura.getSirketId() == null) return;
        String kaynakTip = satis ? KAYNAK_SATIS_FATURA : KAYNAK_ALIS_FATURA;
        try {
            if (fisVarMi(fatura.getSirketId(), kaynakTip, fatura.getId())) return;

            BigDecimal matrah = nz(fatura.getAraToplam());
            BigDecimal kdv = nz(fatura.getKdv());
            BigDecimal toplam = nz(fatura.getGenelToplam());
            if (toplam.signum() == 0 && matrah.signum() == 0) return;

            String aciklama = "Fatura #" + fatura.getFaturaNumarasi();
            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            if (satis) {
                hesapGaranti(fatura.getSirketId(), HESAP_ALICILAR, "Alıcılar", "AKTIF");
                hesapGaranti(fatura.getSirketId(), HESAP_YURTICI_SATISLAR, "Yurtiçi Satışlar", "GELIR");
                hesapGaranti(fatura.getSirketId(), HESAP_HESAPLANAN_KDV, "Hesaplanan KDV", "PASIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ALICILAR).borc(toplam).aciklama(aciklama).build());
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_YURTICI_SATISLAR).alacak(matrah).aciklama(aciklama).build());
                if (kdv.signum() != 0) {
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_HESAPLANAN_KDV).alacak(kdv).aciklama(aciklama).build());
                }
            } else {
                hesapGaranti(fatura.getSirketId(), HESAP_TICARI_MALLAR, "Ticari Mallar", "AKTIF");
                hesapGaranti(fatura.getSirketId(), HESAP_INDIRILECEK_KDV, "İndirilecek KDV", "AKTIF");
                hesapGaranti(fatura.getSirketId(), HESAP_SATICILAR, "Satıcılar", "PASIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_TICARI_MALLAR).borc(matrah).aciklama(aciklama).build());
                if (kdv.signum() != 0) {
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_INDIRILECEK_KDV).borc(kdv).aciklama(aciklama).build());
                }
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATICILAR).alacak(toplam).aciklama(aciklama).build());
            }

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(fatura.getTarih() != null ? fatura.getTarih() : LocalDate.now())
                    .aciklama(aciklama)
                    .sirketId(fatura.getSirketId())
                    .kalemler(kalemler)
                    .kaynakTip(kaynakTip)
                    .kaynakId(fatura.getId())
                    .build());
            log.info("Fatura yevmiye fişi oluşturuldu - Fatura ID: {}, tip: {}", fatura.getId(), kaynakTip);
        } catch (Exception e) {
            log.warn("Fatura otomatik muhasebe fişi oluşturulamadı (fatura id: {}): {}", fatura.getId(), e.getMessage());
        }
    }

    /**
     * Tahsilat yevmiye fişi: Borç 100 (Kasa) veya 102 (Banka) / Alacak 120 (Alıcılar).
     * Nakit hesabı (kasa veya banka) seçilmemişse karşı hesap bilinmediğinden fiş üretilmez
     * (yalnız cari hareket kaydedilir).
     */
    @Transactional
    public void tahsilatIsle(Long sirketId, Long kaynakId, BigDecimal tutar, LocalDate tarih,
                             Long kasaId, Long bankaId, String cariAd) {
        if (sirketId == null || kaynakId == null || tutar == null || tutar.signum() == 0) return;
        // Karşı hesap (kasa/banka) yoksa dengeli bir yevmiye fişi kurulamaz.
        if (kasaId == null && bankaId == null) return;
        try {
            if (fisVarMi(sirketId, KAYNAK_TAHSILAT, kaynakId)) return;

            boolean kasaMi = kasaId != null;
            String borcHesap = kasaMi ? HESAP_KASA : HESAP_BANKA;
            String borcAd = kasaMi ? "Kasa" : "Bankalar";
            hesapGaranti(sirketId, borcHesap, borcAd, "AKTIF");
            hesapGaranti(sirketId, HESAP_ALICILAR, "Alıcılar", "AKTIF");

            String aciklama = "Tahsilat" + (cariAd != null ? " - " + cariAd : "");
            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(borcHesap).borc(tutar).aciklama(aciklama).build());
            kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ALICILAR).alacak(tutar).aciklama(aciklama).build());

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(tarih != null ? tarih : LocalDate.now())
                    .aciklama(aciklama)
                    .sirketId(sirketId)
                    .kalemler(kalemler)
                    .kaynakTip(KAYNAK_TAHSILAT)
                    .kaynakId(kaynakId)
                    .build());
            log.info("Tahsilat yevmiye fişi oluşturuldu - Kaynak ID: {}", kaynakId);
        } catch (Exception e) {
            log.warn("Tahsilat otomatik muhasebe fişi oluşturulamadı (kaynak id: {}): {}", kaynakId, e.getMessage());
        }
    }

    /** Bir kaynağa bağlı aktif (IPTAL olmayan) fiş var mı? */
    private boolean fisVarMi(Long sirketId, String kaynakTip, Long kaynakId) {
        return muhasebeFisiRepository
                .findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(sirketId, kaynakTip, kaynakId, "IPTAL")
                .isPresent();
    }

    /** Bir kaynağa bağlı aktif fişi iptal eder (belge iptali/geri alımı için). */
    @Transactional
    public void kaynakFisIptal(Long sirketId, String kaynakTip, Long kaynakId) {
        if (sirketId == null || kaynakId == null) return;
        try {
            muhasebeFisiRepository
                    .findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(sirketId, kaynakTip, kaynakId, "IPTAL")
                    .ifPresent(f -> {
                        f.setDurum("IPTAL");
                        muhasebeFisiRepository.save(f);
                        log.info("Otomatik yevmiye fişi iptal edildi - Fiş: {}, kaynak: {}/{}", f.getFisNo(), kaynakTip, kaynakId);
                    });
        } catch (Exception e) {
            log.warn("Otomatik fiş iptal edilemedi ({}:{}): {}", kaynakTip, kaynakId, e.getMessage());
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
