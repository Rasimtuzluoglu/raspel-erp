package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.service.sistem.BildirimService;

/**
 * Hareket Service
 * Hareket işlemlerinin business logic'ini yönetir.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HareketService {
    
    private final HareketRepository hareketRepository;
    private final CariHesapRepository cariHesapRepository;
    private final CariHesapService cariHesapService;
    private final BildirimService bildirimService;
        private final FaturaRepository faturaRepository;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    private final com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    private final com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    private final com.raspel.erp.repository.finans.BankaRepository bankaRepository;

    /**
     * Faturanın ödenen tutarını ve ödeme durumunu günceller.
     * delta: TAHSILAT/ODEME hareketi için pozitif, silme/güncelleme tersi için negatif.
     */
    private void faturaOdemeUygula(Long faturaId, BigDecimal delta, String aciklama) {
        if (faturaId == null || delta == null || delta.compareTo(BigDecimal.ZERO) == 0) return;
        // REDTEAM (yarış koşulu): Kilitli okuma. Kilit olmadan iki paralel tahsilat
        // isteği aynı odenenTutar'ı görüp ikisi de kendi toplamını yazıyordu
        // (kanıt: 600 TL'lik faturaya 2x500 TL -> odenen_tutar=1000).
        Fatura fatura = faturaRepository.findByIdForUpdate(faturaId)
                .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + faturaId));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        BigDecimal yeniOdenen = (fatura.getOdenenTutar() != null ? fatura.getOdenenTutar() : BigDecimal.ZERO).add(delta);
        if (yeniOdenen.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Fatura ödenen tutarı negatif olamaz. Ödenen: "
                    + fatura.getOdenenTutar() + ", İşlem: " + delta + " (" + aciklama + ")");
        }
        BigDecimal toplam = fatura.getGenelToplam() != null ? fatura.getGenelToplam() : BigDecimal.ZERO;
        BigDecimal kalan = toplam.subtract(yeniOdenen);
        if (kalan.compareTo(BigDecimal.ZERO) < 0) {
            // Fazla ödeme sessizce yutulmasın: kalan 0'a kırpılıyor ama
            // avans/fazla ödeme kaydı mutlaka görünür olsun (muhasebe izlenebilirliği).
            log.warn("Fazla ödeme tespit edildi. faturaId={} genelToplam={} odenen={} fazla={} islem={}",
                    faturaId, toplam, yeniOdenen, kalan.negate(), aciklama);
        }
        fatura.setOdenenTutar(yeniOdenen);
        fatura.setKalanTutar(kalan.max(BigDecimal.ZERO));
        fatura.setOdemeDurumu(kalan.compareTo(BigDecimal.ZERO) <= 0 ? "ODENDI"
                : yeniOdenen.compareTo(BigDecimal.ZERO) > 0 ? "KISMI_ODENDI" : "ODENMEDI");
        faturaRepository.save(fatura);
        cacheYardimci.commitSonrasiTemizle("faturalar", "dashboard");
    }
    
    /**
     * Belirli bir cari hesaba ait hareketleri getir
     */
    @Transactional(readOnly = true)
    public List<HareketDTO> cariHesapHareketleriGetir(Long cariHesapId) {
        log.debug("Cari hesap hareketleri getiriliyor - ID: {}", cariHesapId);
        
        // Cari hesabın var olduğunu ve geçerli firmaya ait olduğunu kontrol et
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        
        return hareketRepository.findByCariHesapIdOrderByHareketTarihiDesc(cariHesapId)
                .stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }

    /** Opt-in sunucu taraflı sayfalama (page/size verildiğinde çağrılır). */
    @Transactional(readOnly = true)
    public Page<HareketDTO> cariHesapHareketleriGetir(Long cariHesapId, Pageable pageable) {
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        return hareketRepository.findByCariHesapId(cariHesapId, pageable).map(this::entityDTOyeCevir);
    }
    
    /**
     * Son n hareketi getir (tenant filtreli).
     */
    @Transactional(readOnly = true)
    public List<HareketDTO> sonHareketleriGetir(int limit, Long sirketId) {
        Pageable pageable = PageRequest.of(0, limit);
        return hareketRepository.findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(sirketId, pageable)
                .stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni hareket oluştur ve cari hesabın bakiyesini güncelle
     */
    // REDTEAM (yarış koşulu): Bu metot @Transactional DEĞİLDİ; bu yüzden
    // faturaOdemeUygula içindeki PESSIMISTIC_WRITE kilidi her repository
    // çağrısının transaction'ı kapanınca BIRAKILIYORDU — yani kilit koruma
    // sağlamıyordu. Oku-değiştir-yaz döngüsü (odenenTutar) tek transaction
    // içine alınınca eşzamanlı tahsilatlar seri hale gelir.
    @Transactional
    public HareketDTO hareketOlustur(HareketDTO dto, Long sirketId) {
        log.info("Yeni hareket oluşturuluyor - Cari ID: {}, Tür: {}, Tutar: {}, sirketId: {}", 
                dto.getCariHesapId(), dto.getTur(), dto.getTutar(), sirketId);
        
        // Cari hesabın var olduğunu ve geçerli firmaya ait olduğunu kontrol et
        CariHesap cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariHesapId()));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        // Kilitli döneme hareket yazılamaz.
        donemService.kilitKontrol(cariHesap.getSirketId(),
                dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now(), "hareket oluşturma");

        // Bağlı fatura varsa önceden doğrula (fatura şirketi ile eşleşmeli)
        if (dto.getFaturaId() != null) {
            // REDTEAM (yarış koşulu): doğrulama da kilitli okumadan yapılır; aksi
            // halde doğrulama ile ödeme uygulaması farklı satır sürümlerini
            // görebilir (kontrol edilen değil, uygulanan tutar eski olur).
            Fatura fatura = faturaRepository.findByIdForUpdate(dto.getFaturaId())
                    .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + dto.getFaturaId()));
            tenantChecker.check(fatura.getSirketId(), "Fatura");
            if (fatura.getCariHesap() != null && !fatura.getCariHesap().getId().equals(dto.getCariHesapId())) {
                throw new BusinessException("Fatura bu cari hesaba ait değil");
            }
        }
        
        // Hareket türünü valide et
        Hareket.HareketTuru hareketTuru;
        try {
            hareketTuru = Hareket.HareketTuru.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz hareket türü: " + dto.getTur());
        }
        
        // Bakiye güncelleme tutarını hesapla.
        // Bakiye gösterimi: pozitif = alacak, negatif = borç (kullanıcıya eksi olarak görünür).
        // Satış faturası kesilince cari borçlanır (bakiye azalır/negatif), tahsilat alınınca bakiye artar (borç kapanır).
        // BORC (borçlandırma) da satış gibi cariyi borçlandırır (bakiye negatife çekilir).
        boolean borclandirma = hareketTuru == Hareket.HareketTuru.BORC;
        BigDecimal bakiyeGuncellemeTutari = hareketTuru == Hareket.HareketTuru.TAHSILAT
                ? dto.getTutar()
                : dto.getTutar().negate();

        // Ödeme yöntemi geçerli değilse reddet (borçlandırmada ödeme yöntemi/taksit yoktur)
        String odemeYontemi = borclandirma ? null : odemeYontemiDogrula(dto.getOdemeYontemi());
        // Taksit için kurum ve tutar zorunlu
        if ("TAKSIT".equals(odemeYontemi)
                && (dto.getTaksitKurum() == null || dto.getTaksitKurum().isBlank()
                || dto.getTaksitTutar() == null || dto.getTaksitTutar().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("Taksit seçildiğinde taksit kurumu ve çekilen tutar girilmelidir");
        }

        // Borçlandırma bir tahsilat/ödeme değildir; faturaya bağlanmaz.
        Long bagliFaturaId = borclandirma ? null : dto.getFaturaId();

        // Hareket oluştur
        Hareket hareket = Hareket.builder()
                .cariHesap(cariHesap)
                .tur(hareketTuru)
                .tutar(dto.getTutar())
                .hareketTarihi(dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now())
                .aciklama(dto.getAciklama())
                .odemeSekli(dto.getOdemeSekli())
                .odemeYontemi(odemeYontemi)
                .taksitKurum(dto.getTaksitKurum())
                .taksitTutar(dto.getTaksitTutar())
                .posTerminaliId(dto.getPosTerminaliId())
                .posAd(dto.getPosAd())
                .komisyonTutar(dto.getKomisyonTutar())
                .valorTarihi(dto.getValorTarihi())
                .faturaId(bagliFaturaId)
                .sirketId(sirketId)
                .build();
        
        Hareket kaydedilenHareket = hareketRepository.save(hareket);
        
        // Cari hesabın bakiyesini güncelle
        cariHesapService.bakiyeGuncelle(dto.getCariHesapId(), bakiyeGuncellemeTutari);

        // Faturaya işle (varsa): ödenen tutar artar
        if (bagliFaturaId != null) {
            faturaOdemeUygula(bagliFaturaId, dto.getTutar(), "Hareket #" + kaydedilenHareket.getId());
        }
        
        if (sirketId != null && !borclandirma) {
            Long bildirimSirketId = sirketId;
            String turAdi = hareketTuru == Hareket.HareketTuru.TAHSILAT ? "TAKSILAT" : "ODEME";
            String baslik = (hareketTuru == Hareket.HareketTuru.TAHSILAT ? "Tahsilat: " : "Ödeme: ") + dto.getTutar() + " ₺";
            String aciklama = cariHesap.getAd() + (dto.getAciklama() != null ? " - " + dto.getAciklama() : "");
            com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, turAdi, baslik, aciklama));
        }
        
        log.info("Hareket başarıyla oluşturuldu - ID: {}", kaydedilenHareket.getId());
        
        return entityDTOyeCevir(kaydedilenHareket);
    }
    
    @Transactional(readOnly = true)
    public Page<HareketDTO> tumHareketleriGetir(Long sirketId, Pageable pageable) {
        log.debug("Tüm hareketler getiriliyor, sirketId: {}", sirketId);
        return hareketRepository.findBySirketIdOrderByHareketTarihiDesc(sirketId, pageable)
                .map(this::entityDTOyeCevir);
    }

    /**
     * Tarih aralığına göre hareketleri filtrele (tenant filtreli)
     */
    @Transactional(readOnly = true)
    public Page<HareketDTO> hareketleriFiltrele(Long cariHesapId, LocalDate baslangic, LocalDate bitis, Pageable pageable, Long sirketId) {
        log.debug("Hareketler filtreleniyor - Cari: {}, Tarih: {} - {}, sirketId: {}", cariHesapId, baslangic, bitis, sirketId);

        if (baslangic == null) baslangic = LocalDate.of(2000, 1, 1);
        if (bitis == null) bitis = LocalDate.now().plusDays(1);

        Page<Hareket> sonuc;
        if (cariHesapId != null) {
            CariHesap cari = cariHesapRepository.findById(cariHesapId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
            tenantChecker.check(cari.getSirketId(), "Cari Hesap");
            sonuc = hareketRepository.findBySirketIdAndCariHesapIdAndHareketTarihiBetween(sirketId, cariHesapId, baslangic, bitis, pageable);
        } else {
            sonuc = hareketRepository.findBySirketIdAndHareketTarihiBetween(sirketId, baslangic, bitis, pageable);
        }

        return sonuc.map(this::entityDTOyeCevir);
    }

    /**
     * Hareket güncelle
     */
    @Transactional
    public HareketDTO hareketGuncelle(Long id, HareketDTO dto) {
        log.info("Hareket güncelleniyor - ID: {}", id);

        Hareket hareket = hareketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", id));
        tenantChecker.check(hareket.getSirketId(), "Hareket");

        CariHesap cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariHesapId()));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        // Eski ve yeni tarih kilitli döneme denk gelmemeli.
        donemService.kilitKontrol(hareket.getSirketId(), hareket.getHareketTarihi(), "hareket güncelleme");
        if (dto.getHareketTarihi() != null) {
            donemService.kilitKontrol(cariHesap.getSirketId(), dto.getHareketTarihi(), "hareket güncelleme");
        }

        if (dto.getFaturaId() != null) {
            Fatura yeniFatura = faturaRepository.findById(dto.getFaturaId())
                    .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + dto.getFaturaId()));
            tenantChecker.check(yeniFatura.getSirketId(), "Fatura");
        }

        Hareket.HareketTuru yeniTur;
        try {
            yeniTur = Hareket.HareketTuru.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz hareket türü: " + dto.getTur());
        }

        // Eski cari, hareket.setCariHesap ile ezilmeden önce saklanmalı; aksi halde
        // cari değiştirildiğinde eski carinin bakiyesi düzeltilmez (delta 0 olur).
        Long eskiCariId = hareket.getCariHesap() != null ? hareket.getCariHesap().getId() : null;

        BigDecimal eskiBakiyeEtkisi = hareket.getTur() == Hareket.HareketTuru.TAHSILAT
                ? hareket.getTutar() : hareket.getTutar().negate();

        BigDecimal yeniBakiyeEtkisi = yeniTur == Hareket.HareketTuru.TAHSILAT
                ? dto.getTutar() : dto.getTutar().negate();

        // Eski bağlı fatura etkisini geri al, yeni faturaya uygula
        Long eskiFaturaId = hareket.getFaturaId();
        if (eskiFaturaId != null) {
            faturaOdemeUygula(eskiFaturaId, hareket.getTutar().negate(), "Hareket #" + hareket.getId() + " güncellendi");
        }
        if (dto.getFaturaId() != null && !dto.getFaturaId().equals(eskiFaturaId)) {
            faturaOdemeUygula(dto.getFaturaId(), dto.getTutar(), "Hareket #" + hareket.getId() + " güncellendi");
        } else if (dto.getFaturaId() != null) {
            faturaOdemeUygula(dto.getFaturaId(), dto.getTutar(), "Hareket #" + hareket.getId() + " güncellendi");
        }

        hareket.setCariHesap(cariHesap);
        hareket.setTur(yeniTur);
        hareket.setTutar(dto.getTutar());
        hareket.setHareketTarihi(dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now());
        hareket.setAciklama(dto.getAciklama());
        if (dto.getOdemeSekli() != null) hareket.setOdemeSekli(dto.getOdemeSekli());
        if (dto.getOdemeYontemi() != null) hareket.setOdemeYontemi(odemeYontemiDogrula(dto.getOdemeYontemi()));
        if (dto.getTaksitKurum() != null) hareket.setTaksitKurum(dto.getTaksitKurum());
        if (dto.getTaksitTutar() != null) hareket.setTaksitTutar(dto.getTaksitTutar());
        hareket.setFaturaId(dto.getFaturaId());

        Hareket guncellenen = hareketRepository.save(hareket);

        Long yeniCariId = hareket.getCariHesap() != null ? hareket.getCariHesap().getId() : null;
        if (eskiCariId != null && !eskiCariId.equals(yeniCariId)) {
            // Cari taşındı: eski cariden eski etkiyi geri al, yeni cariye yeni etkiyi uygula.
            cariHesapService.bakiyeGuncelle(eskiCariId, eskiBakiyeEtkisi.negate());
            if (yeniCariId != null) {
                cariHesapService.bakiyeGuncelle(yeniCariId, yeniBakiyeEtkisi);
            }
        } else if (yeniCariId != null) {
            cariHesapService.bakiyeGuncelle(yeniCariId, yeniBakiyeEtkisi.subtract(eskiBakiyeEtkisi));
        }

        log.info("Hareket başarıyla güncellendi - ID: {}", id);
        return entityDTOyeCevir(guncellenen);
    }

    /**
     * Hareket sil (ve bakiyeyi ters işlemle güncelle)
     */
    @Transactional
    public void hareketSil(Long id) {
        log.info("Hareket siliniliyor - ID: {}", id);
        
        Hareket hareket = hareketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", id));
        tenantChecker.check(hareket.getSirketId(), "Hareket");
        donemService.kilitKontrol(hareket.getSirketId(), hareket.getHareketTarihi(), "hareket silme");
        
        // Bakiye güncellemeyi ters işlemle yap (tahsilat silinirse bakiye azalır, ödeme silinirse artar)
        BigDecimal bakiyeGuncellemeTutari = hareket.getTur() == Hareket.HareketTuru.TAHSILAT 
                ? hareket.getTutar().negate()
                : hareket.getTutar();
        
        cariHesapService.bakiyeGuncelle(hareket.getCariHesap().getId(), bakiyeGuncellemeTutari);

        // Bağlı fatura varsa ödenen tutarı geri al
        if (hareket.getFaturaId() != null) {
            faturaOdemeUygula(hareket.getFaturaId(), hareket.getTutar().negate(), "Hareket #" + hareket.getId() + " silindi");
        }

        // Tahsilata bağlı kasa/banka hareketi varsa ters kaydet; aksi halde cari düzelirken
        // kasa/banka bakiyesi şişer (mutabakat bozulur).
        int tersKasaBanka = kasaBankaTersKaydet(hareket);
        
        auditLogService.finansalSilmeLog("Hareket", id,
                "Hareket silindi: " + hareket.getTur() + " " + hareket.getTutar() + " TL - Cari: "
                        + hareket.getCariHesap().getAd() + " (bakiye terslendi)"
                        + (hareket.getFaturaId() != null ? " - Fatura: " + hareket.getFaturaId() : "")
                        + (tersKasaBanka > 0 ? " - Bağlı kasa/banka hareketi ters kaydedildi (" + tersKasaBanka + ")" : ""));
        
        hareketRepository.deleteById(id);
        log.info("Hareket başarıyla silindi - ID: {}", id);
    }

    /**
     * Cari harekete bağlı kasa/banka hareketlerini siler ve bakiyeleri düzeltir.
     * Bağlantı: kaynakTip='TAHSILAT', kaynakId=<cari hareket id>.
     */
    private int kasaBankaTersKaydet(Hareket hareket) {
        int sayi = 0;
        try {
            for (var kh : kasaHareketRepository.findByKaynakTipAndKaynakId("TAHSILAT", hareket.getId())) {
                var kasa = kh.getKasa();
                if (kasa != null) {
                    BigDecimal tutar = kh.getTutar() != null ? kh.getTutar() : BigDecimal.ZERO;
                    kasa.setBakiye((kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO).subtract(tutar));
                    kasaRepository.save(kasa);
                }
                kasaHareketRepository.delete(kh);
                sayi++;
            }
            for (var bh : bankaHareketiRepository.findByKaynakTipAndKaynakId("TAHSILAT", hareket.getId())) {
                var banka = bh.getBankaId() != null ? bankaRepository.findById(bh.getBankaId()).orElse(null) : null;
                if (banka != null) {
                    BigDecimal tutar = bh.getAlacak() != null ? bh.getAlacak() : BigDecimal.ZERO;
                    banka.setBakiye((banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO).subtract(tutar));
                    bankaRepository.save(banka);
                }
                bankaHareketiRepository.delete(bh);
                sayi++;
            }
        } catch (Exception e) {
            log.warn("Bağlı kasa/banka hareketi ters kaydedilemedi (hareket id: {}): {}", hareket.getId(), e.getMessage());
        }
        return sayi;
    }
    
    /**
     * Entity'yi DTO'ya çevir
     */
    public HareketDTO entityDTOyeCevir(Hareket hareket) {
        return HareketDTO.builder()
                .id(hareket.getId())
                .cariHesapId(hareket.getCariHesap().getId())
                .cariHesapAd(hareket.getCariHesap().getAd())
                .tur(hareket.getTur().name())
                .tutar(hareket.getTutar())
                .hareketTarihi(hareket.getHareketTarihi())
                .aciklama(hareket.getAciklama())
                .odemeSekli(hareket.getOdemeSekli())
                .odemeYontemi(hareket.getOdemeYontemi())
                .taksitKurum(hareket.getTaksitKurum())
                .taksitTutar(hareket.getTaksitTutar())
                .posTerminaliId(hareket.getPosTerminaliId())
                .posAd(hareket.getPosAd())
                .komisyonTutar(hareket.getKomisyonTutar())
                .valorTarihi(hareket.getValorTarihi())
                .faturaId(hareket.getFaturaId())
                .olusturmaTarihi(hareket.getOlusturmaTarihi())
                .build();
    }

    /** Ödeme yöntemini doğrular; geçersizse null döner. */
    private String odemeYontemiDogrula(String odemeYontemi) {
        if (odemeYontemi == null || odemeYontemi.isBlank()) return null;
        String yontem = odemeYontemi.toUpperCase();
        return java.util.Set.of("NAKIT", "KART", "TAKSIT", "HAVALE").contains(yontem) ? yontem : null;
    }
}