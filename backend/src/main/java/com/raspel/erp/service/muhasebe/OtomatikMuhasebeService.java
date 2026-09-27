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
    public static final String KAYNAK_IADE = "IADE";
    public static final String KAYNAK_CEK_SENET = "CEK_SENET";
    public static final String KAYNAK_YIL_SONU = "YIL_SONU_KAPANIS";
    public static final String KAYNAK_MASRAF = "MASRAF";
    public static final String KAYNAK_FX_DEGERLEME = "FX_DEGERLEME";

    private static final String HESAP_YONETIM_GIDER = "770";
    private static final String HESAP_PERSONELE_BORCLAR = "335";
    private static final String HESAP_ODENECEK_VERGI = "360";
    // Ticari standart hesap eşlemesi
    private static final String HESAP_KASA = "100";
    private static final String HESAP_ALINAN_CEKLER = "101";
    private static final String HESAP_BANKA = "102";
    private static final String HESAP_ALICILAR = "120";
    private static final String HESAP_ALACAK_SENETLERI = "121";
    private static final String HESAP_TICARI_MALLAR = "153";
    private static final String HESAP_INDIRILECEK_KDV = "191";
    private static final String HESAP_SATICILAR = "320";
    private static final String HESAP_HESAPLANAN_KDV = "391";
    private static final String HESAP_YURTICI_SATISLAR = "600";
    private static final String HESAP_SATISTAN_IADELER = "610";
    private static final String HESAP_SATILAN_MALIN_MALIYETI = "621";
    private static final String HESAP_DONEM_KARI = "690";
    private static final String HESAP_GECMIS_YIL_KARLARI = "570";
    private static final String HESAP_ONCEKI_YILLAR_ZARARLARI = "580";
    private static final String HESAP_KAMBIYO_KARLARI = "646";
    private static final String HESAP_KAMBIYO_ZARARLARI = "656";

    private final MuhasebeService muhasebeService;
    private final MuhasebeFisiRepository muhasebeFisiRepository;
    private final HesapPlaniRepository hesapPlaniRepository;
    private final com.raspel.erp.repository.muhasebe.MuhasebeFisKalemRepository muhasebeFisKalemRepository;
    private final com.raspel.erp.repository.envanter.StokMaliyetHareketRepository stokMaliyetHareketRepository;
    private final com.raspel.erp.repository.ticaret.IadeKalemRepository iadeKalemRepository;
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    private final com.raspel.erp.service.sistem.TcmbKurService tcmbKurService;

    @org.springframework.beans.factory.annotation.Value("${app.kdv.varsayilan-oran:20}")
    private BigDecimal varsayilanKdvOrani;

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
                // Satılan malın maliyeti: stok maliyet defterinden (FATURA veya kaynak irsaliye).
                BigDecimal cogs = satisCogsToplam(fatura);
                if (cogs.signum() > 0) {
                    hesapGaranti(fatura.getSirketId(), HESAP_SATILAN_MALIN_MALIYETI, "Satılan Malın Maliyeti", "GIDER");
                    hesapGaranti(fatura.getSirketId(), HESAP_TICARI_MALLAR, "Ticari Mallar", "AKTIF");
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATILAN_MALIN_MALIYETI)
                            .borc(cogs).aciklama("SMM - " + aciklama).build());
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_TICARI_MALLAR)
                            .alacak(cogs).aciklama("SMM - " + aciklama).build());
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

    /** Satış faturasının satılan malın maliyeti (stok maliyet defteri; kaynak irsaliye dahil). */
    private BigDecimal satisCogsToplam(com.raspel.erp.entity.ticaret.Fatura fatura) {
        BigDecimal toplam = BigDecimal.ZERO;
        for (var m : stokMaliyetHareketRepository.findByKaynakTipAndKaynakId("FATURA", fatura.getId())) {
            if ("CIKIS".equals(m.getTur()) && m.getToplamMaliyet() != null) {
                toplam = toplam.add(m.getToplamMaliyet());
            }
        }
        if (fatura.getIrsaliyeId() != null) {
            for (var m : stokMaliyetHareketRepository.findByKaynakTipAndKaynakId("IRSALIYE", fatura.getIrsaliyeId())) {
                if ("CIKIS".equals(m.getTur()) && m.getToplamMaliyet() != null) {
                    toplam = toplam.add(m.getToplamMaliyet());
                }
            }
        }
        return toplam;
    }

    /**
     * İade yevmiye fişi. Satış iadesi: Borç 610 + Borç 391 / Alacak 120; iade edilen malın
     * maliyeti stoğa geri girdiği için ayrıca Borç 153 / Alacak 621. Alış iadesi:
     * Borç 320 / Alacak 153 + 191. Idempotenttir.
     */
    @Transactional
    public void iadeIsle(com.raspel.erp.entity.ticaret.Iade iade) {
        if (iade == null || iade.getId() == null || iade.getSirketId() == null) return;
        try {
            if (fisVarMi(iade.getSirketId(), KAYNAK_IADE, iade.getId())) return;
            boolean satisIadesi = !"ALIS".equals(iade.getTur());

            BigDecimal net = BigDecimal.ZERO;
            BigDecimal kdv = BigDecimal.ZERO;
            BigDecimal brut = BigDecimal.ZERO;
            for (var k : iadeKalemRepository.findByIadeId(iade.getId())) {
                var s = com.raspel.erp.util.FaturaTutar.satir(k.getBirimFiyat(), k.getMiktar(),
                        BigDecimal.ZERO, k.getKdvOrani());
                net = net.add(s.net());
                kdv = kdv.add(s.kdv());
                brut = brut.add(s.brut());
            }
            if (brut.signum() == 0) {
                brut = nz(iade.getTutar());
                if (brut.signum() == 0) return;
                // Kalemsiz iade: KDV varsayılan orandan ayrıştırılır (tutar KDV dahil kabul edilir).
                BigDecimal oran = BigDecimal.ONE.add(
                        nz(varsayilanKdvOrani).divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP));
                net = brut.divide(oran, 2, java.math.RoundingMode.HALF_UP);
                kdv = brut.subtract(net);
            }

            String aciklama = "İade #" + iade.getId();
            List<MuhasebeFisKalemDTO> fisKalemleri = new ArrayList<>();
            if (satisIadesi) {
                hesapGaranti(iade.getSirketId(), HESAP_SATISTAN_IADELER, "Satıştan İadeler", "GIDER");
                hesapGaranti(iade.getSirketId(), HESAP_HESAPLANAN_KDV, "Hesaplanan KDV", "PASIF");
                hesapGaranti(iade.getSirketId(), HESAP_ALICILAR, "Alıcılar", "AKTIF");
                fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATISTAN_IADELER)
                        .borc(net).aciklama(aciklama).build());
                if (kdv.signum() != 0) {
                    fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_HESAPLANAN_KDV)
                            .borc(kdv).aciklama(aciklama).build());
                }
                fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ALICILAR)
                        .alacak(brut).aciklama(aciklama).build());

                BigDecimal maliyet = iadeMaliyetToplam(iade.getId());
                if (maliyet.signum() > 0) {
                    hesapGaranti(iade.getSirketId(), HESAP_TICARI_MALLAR, "Ticari Mallar", "AKTIF");
                    hesapGaranti(iade.getSirketId(), HESAP_SATILAN_MALIN_MALIYETI, "Satılan Malın Maliyeti", "GIDER");
                    fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_TICARI_MALLAR)
                            .borc(maliyet).aciklama("İade SMM - " + aciklama).build());
                    fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATILAN_MALIN_MALIYETI)
                            .alacak(maliyet).aciklama("İade SMM - " + aciklama).build());
                }
            } else {
                hesapGaranti(iade.getSirketId(), HESAP_SATICILAR, "Satıcılar", "PASIF");
                hesapGaranti(iade.getSirketId(), HESAP_TICARI_MALLAR, "Ticari Mallar", "AKTIF");
                hesapGaranti(iade.getSirketId(), HESAP_INDIRILECEK_KDV, "İndirilecek KDV", "AKTIF");
                fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATICILAR)
                        .borc(brut).aciklama(aciklama).build());
                fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_TICARI_MALLAR)
                        .alacak(net).aciklama(aciklama).build());
                if (kdv.signum() != 0) {
                    fisKalemleri.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_INDIRILECEK_KDV)
                            .alacak(kdv).aciklama(aciklama).build());
                }
            }

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(iade.getTarih() != null ? iade.getTarih() : LocalDate.now())
                    .aciklama(aciklama)
                    .sirketId(iade.getSirketId())
                    .kalemler(fisKalemleri)
                    .kaynakTip(KAYNAK_IADE)
                    .kaynakId(iade.getId())
                    .build());
            log.info("İade yevmiye fişi oluşturuldu - İade ID: {}", iade.getId());
        } catch (Exception e) {
            log.warn("İade otomatik muhasebe fişi oluşturulamadı (iade id: {}): {}", iade.getId(), e.getMessage());
        }
    }

    /** İade ile stoğa geri giren malın maliyet toplamı. */
    private BigDecimal iadeMaliyetToplam(Long iadeId) {
        BigDecimal toplam = BigDecimal.ZERO;
        for (var m : stokMaliyetHareketRepository.findByKaynakTipAndKaynakId("IADE", iadeId)) {
            if ("GIRIS".equals(m.getTur()) && m.getToplamMaliyet() != null) {
                toplam = toplam.add(m.getToplamMaliyet());
            }
        }
        return toplam;
    }

    /**
     * Çek/senet tahsil yevmiye fişi: Borç 100 (Kasa) veya 102 (Banka) / Alacak 120 (Alıcılar).
     * Nakit hesabı seçilmemişse karşı hesap bilinmediğinden fiş üretilmez. Idempotenttir.
     */
    @Transactional
    public void cekSenetTahsilIsle(com.raspel.erp.entity.finans.CekSenet cs, Long kasaId, Long bankaId) {
        if (cs == null || cs.getId() == null || cs.getSirketId() == null) return;
        if (kasaId == null && bankaId == null) return;
        if (cs.getTutar() == null || cs.getTutar().signum() == 0) return;
        try {
            if (fisVarMi(cs.getSirketId(), KAYNAK_CEK_SENET, cs.getId())) return;
            boolean kasaMi = kasaId != null;
            String borcHesap = kasaMi ? HESAP_KASA : HESAP_BANKA;
            hesapGaranti(cs.getSirketId(), borcHesap, kasaMi ? "Kasa" : "Bankalar", "AKTIF");
            hesapGaranti(cs.getSirketId(), HESAP_ALICILAR, "Alıcılar", "AKTIF");

            String aciklama = "Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : "");
            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(borcHesap).borc(cs.getTutar()).aciklama(aciklama).build());
            kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ALICILAR).alacak(cs.getTutar()).aciklama(aciklama).build());

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(LocalDate.now())
                    .aciklama(aciklama)
                    .sirketId(cs.getSirketId())
                    .kalemler(kalemler)
                    .kaynakTip(KAYNAK_CEK_SENET)
                    .kaynakId(cs.getId())
                    .build());
            log.info("Çek/senet tahsil yevmiye fişi oluşturuldu - Kayıt ID: {}", cs.getId());
        } catch (Exception e) {
            log.warn("Çek/senet otomatik muhasebe fişi oluşturulamadı (id: {}): {}", cs.getId(), e.getMessage());
        }
    }

    /**
     * Yıl sonu kapanış fişi: 6xx gelir tablosu hesaplarının bakiyeleri 690 (Dönem Kârı/Zararı)
     * hesabına aktarılır; kalan kâr 570 (Geçmiş Yıllar Kârları), zarar 580 (Önceki Yıllar
     * Zararları) hesabına devredilir. Aynı yıl için mükerrer fiş üretilmez.
     */
    @Transactional
    public void yilSonuKapanisFisi(Long sirketId, Integer yil) {
        if (sirketId == null || yil == null) return;
        try {
            if (fisVarMi(sirketId, KAYNAK_YIL_SONU, yil.longValue())) return;
            LocalDate bas = LocalDate.of(yil, 1, 1);
            LocalDate bit = LocalDate.of(yil, 12, 31);

            java.util.Map<String, BigDecimal> netler = new java.util.TreeMap<>();
            for (var k : muhasebeFisKalemRepository.aktifKalemler(sirketId, bas, bit)) {
                String kod = k.getHesapKodu();
                // Yalnızca gelir tablosu hesapları (6xx) kapatılır; 690'ın kendisi hariç.
                if (kod == null || !kod.startsWith("6") || HESAP_DONEM_KARI.equals(kod)) continue;
                netler.merge(kod, nz(k.getBorc()).subtract(nz(k.getAlacak())), BigDecimal::add);
            }
            netler.entrySet().removeIf(e -> e.getValue().signum() == 0);
            if (netler.isEmpty()) return;

            hesapGaranti(sirketId, HESAP_DONEM_KARI, "Dönem Kârı veya Zararı", "PASIF");
            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            BigDecimal net690 = BigDecimal.ZERO;
            for (var e : netler.entrySet()) {
                BigDecimal n = e.getValue();
                if (n.signum() > 0) {
                    // Gider hesabı (borç bakiye): hesap alacaklanır, 690 borçlanır.
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(e.getKey()).alacak(n)
                            .aciklama("Yıl sonu kapanışı " + yil).build());
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_DONEM_KARI).borc(n)
                            .aciklama("Yıl sonu kapanışı " + yil).build());
                } else {
                    // Gelir hesabı (alacak bakiye): hesap borçlandırılır, 690 alacaklanır.
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(e.getKey()).borc(n.negate())
                            .aciklama("Yıl sonu kapanışı " + yil).build());
                    kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_DONEM_KARI).alacak(n.negate())
                            .aciklama("Yıl sonu kapanışı " + yil).build());
                }
                net690 = net690.add(n);
            }
            if (net690.signum() > 0) {
                // Zarar: 580 borç / 690 alacak.
                hesapGaranti(sirketId, HESAP_ONCEKI_YILLAR_ZARARLARI, "Önceki Yıllar Zararları", "AKTIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ONCEKI_YILLAR_ZARARLARI)
                        .borc(net690).aciklama("Yıl sonu zararı " + yil).build());
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_DONEM_KARI)
                        .alacak(net690).aciklama("Yıl sonu zararı " + yil).build());
            } else if (net690.signum() < 0) {
                // Kâr: 690 borç / 570 alacak.
                hesapGaranti(sirketId, HESAP_GECMIS_YIL_KARLARI, "Geçmiş Yıllar Kârları", "PASIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_DONEM_KARI)
                        .borc(net690.negate()).aciklama("Yıl sonu kârı " + yil).build());
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_GECMIS_YIL_KARLARI)
                        .alacak(net690.negate()).aciklama("Yıl sonu kârı " + yil).build());
            }

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(bit)
                    .aciklama("Yıl sonu kapanışı " + yil)
                    .sirketId(sirketId)
                    .kalemler(kalemler)
                    .kaynakTip(KAYNAK_YIL_SONU)
                    .kaynakId(yil.longValue())
                    .build());
            log.info("Yıl sonu kapanış fişi oluşturuldu - Şirket: {}, Yıl: {}", sirketId, yil);
        } catch (Exception e) {
            log.warn("Yıl sonu kapanış fişi oluşturulamadı (şirket {}, yıl {}): {}", sirketId, yil, e.getMessage());
        }
    }

    /**
     * Masraf yevmiye fişi: Borç 770 (matrah) + Borç 191 (KDV) / Alacak 100-102 (ödeme yapıldıysa)
     * veya Alacak 320 (satıcılar). Idempotenttir.
     */
    @Transactional
    public void masrafIsle(com.raspel.erp.entity.finans.Masraf masraf) {
        if (masraf == null || masraf.getId() == null || masraf.getSirketId() == null) return;
        BigDecimal brut = nz(masraf.getTutar());
        if (brut.signum() == 0) return;
        try {
            if (fisVarMi(masraf.getSirketId(), KAYNAK_MASRAF, masraf.getId())) return;
            BigDecimal kdv = nz(masraf.getKdvTutar());
            BigDecimal matrah = masraf.getMatrah() != null ? nz(masraf.getMatrah()) : brut.subtract(kdv);
            if (matrah.signum() == 0 && kdv.signum() == 0) matrah = brut;

            String aciklama = "Masraf" + (masraf.getBelgeNo() != null ? " #" + masraf.getBelgeNo() : "")
                    + (masraf.getAciklama() != null ? " - " + masraf.getAciklama() : "");
            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            hesapGaranti(masraf.getSirketId(), HESAP_YONETIM_GIDER, "Yönetim Giderleri", "GIDER");
            kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_YONETIM_GIDER)
                    .borc(matrah).aciklama(aciklama).build());
            if (kdv.signum() != 0) {
                hesapGaranti(masraf.getSirketId(), HESAP_INDIRILECEK_KDV, "İndirilecek KDV", "AKTIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_INDIRILECEK_KDV)
                        .borc(kdv).aciklama(aciklama).build());
            }
            if (masraf.getKasaId() != null) {
                hesapGaranti(masraf.getSirketId(), HESAP_KASA, "Kasa", "AKTIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_KASA)
                        .alacak(brut).aciklama(aciklama).build());
            } else if (masraf.getBankaId() != null) {
                hesapGaranti(masraf.getSirketId(), HESAP_BANKA, "Bankalar", "AKTIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_BANKA)
                        .alacak(brut).aciklama(aciklama).build());
            } else {
                hesapGaranti(masraf.getSirketId(), HESAP_SATICILAR, "Satıcılar", "PASIF");
                kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATICILAR)
                        .alacak(brut).aciklama(aciklama).build());
            }

            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(masraf.getTarih() != null ? masraf.getTarih() : LocalDate.now())
                    .aciklama(aciklama)
                    .sirketId(masraf.getSirketId())
                    .kalemler(kalemler)
                    .kaynakTip(KAYNAK_MASRAF)
                    .kaynakId(masraf.getId())
                    .build());
            log.info("Masraf yevmiye fişi oluşturuldu - Masraf ID: {}", masraf.getId());
        } catch (Exception e) {
            log.warn("Masraf otomatik muhasebe fişi oluşturulamadı (masraf id: {}): {}", masraf.getId(), e.getMessage());
        }
    }

    /**
     * Dönem sonu kur değerlemesi: kalanı olan dövizli faturalar için kayıt kuru ile güncel
     * TCMB satış kuru arasındaki fark kambiyo kâr/zararı olarak işlenir. Satışta alıcılar
     * (120) değerlenir; alışta satıcılar (320) değerlenir. Şirket + tarih bazında idempotenttir.
     */
    @Transactional
    public void fxDegerlemeFisi(Long sirketId, LocalDate tarih) {
        if (sirketId == null) return;
        LocalDate gun = tarih != null ? tarih : LocalDate.now();
        try {
            if (fisVarMi(sirketId, KAYNAK_FX_DEGERLEME, gun.toEpochDay())) return;
            List<com.raspel.erp.entity.ticaret.Fatura> faturalar =
                    faturaRepository.findBySirketIdAndDurumAndParaBirimiNotAndKalanTutarGreaterThan(
                            sirketId, com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI,
                            "TRY", BigDecimal.ZERO);
            List<MuhasebeFisKalemDTO> kalemler = new ArrayList<>();
            BigDecimal toplamKar = BigDecimal.ZERO;
            BigDecimal toplamZarar = BigDecimal.ZERO;
            for (var f : faturalar) {
                if (f.getKur() == null || f.getKur().signum() <= 0) continue;
                if (f.getKalanTutar() == null || f.getKalanTutar().signum() <= 0) continue;
                BigDecimal guncelKur;
                try {
                    guncelKur = tcmbKurService.cevir(BigDecimal.ONE, f.getParaBirimi(), "TRY");
                } catch (Exception e) {
                    continue;
                }
                if (guncelKur == null || guncelKur.signum() <= 0) continue;
                // Orijinal döviz kalanı = TL kalan / kayıt kuru.
                BigDecimal dovizKalan = f.getKalanTutar().divide(f.getKur(), 4, java.math.RoundingMode.HALF_UP);
                BigDecimal fark = dovizKalan.multiply(guncelKur.subtract(f.getKur()))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                if (fark.abs().compareTo(new BigDecimal("0.01")) < 0) continue;

                boolean satis = f.getTur() == com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS;
                String aciklama = "Kur değerleme - " + f.getFaturaNumarasi() + " (" + f.getParaBirimi() + ")";
                if (satis) {
                    // Varlık değerlenir: artış kâr (120 borç / 646 alacak), azalış zarar (656 borç / 120 alacak).
                    hesapGaranti(sirketId, HESAP_ALICILAR, "Alıcılar", "AKTIF");
                    if (fark.signum() > 0) {
                        hesapGaranti(sirketId, HESAP_KAMBIYO_KARLARI, "Kambiyo Kârları", "GELIR");
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ALICILAR)
                                .borc(fark).aciklama(aciklama).build());
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_KAMBIYO_KARLARI)
                                .alacak(fark).aciklama(aciklama).build());
                        toplamKar = toplamKar.add(fark);
                    } else {
                        hesapGaranti(sirketId, HESAP_KAMBIYO_ZARARLARI, "Kambiyo Zararları", "GIDER");
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_KAMBIYO_ZARARLARI)
                                .borc(fark.negate()).aciklama(aciklama).build());
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_ALICILAR)
                                .alacak(fark.negate()).aciklama(aciklama).build());
                        toplamZarar = toplamZarar.add(fark.negate());
                    }
                } else {
                    // Borç değerlenir: artış zarar (656 borç / 320 alacak), azalış kâr (320 borç / 646 alacak).
                    hesapGaranti(sirketId, HESAP_SATICILAR, "Satıcılar", "PASIF");
                    if (fark.signum() > 0) {
                        hesapGaranti(sirketId, HESAP_KAMBIYO_ZARARLARI, "Kambiyo Zararları", "GIDER");
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_KAMBIYO_ZARARLARI)
                                .borc(fark).aciklama(aciklama).build());
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATICILAR)
                                .alacak(fark).aciklama(aciklama).build());
                        toplamZarar = toplamZarar.add(fark);
                    } else {
                        hesapGaranti(sirketId, HESAP_KAMBIYO_KARLARI, "Kambiyo Kârları", "GELIR");
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_SATICILAR)
                                .borc(fark.negate()).aciklama(aciklama).build());
                        kalemler.add(MuhasebeFisKalemDTO.builder().hesapKodu(HESAP_KAMBIYO_KARLARI)
                                .alacak(fark.negate()).aciklama(aciklama).build());
                        toplamKar = toplamKar.add(fark.negate());
                    }
                }
            }
            if (kalemler.isEmpty()) return;
            muhasebeService.fisOlustur(MuhasebeFisiDTO.builder()
                    .tarih(gun)
                    .aciklama("Dönem sonu kur değerlemesi (" + gun + ")")
                    .sirketId(sirketId)
                    .kalemler(kalemler)
                    .kaynakTip(KAYNAK_FX_DEGERLEME)
                    .kaynakId(gun.toEpochDay())
                    .build());
            log.info("Kur değerleme fişi oluşturuldu - Şirket: {}, tarih: {}, kâr: {}, zarar: {}",
                    sirketId, gun, toplamKar, toplamZarar);
        } catch (Exception e) {
            log.warn("Kur değerleme fişi oluşturulamadı (şirket {}, tarih {}): {}", sirketId, gun, e.getMessage());
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
