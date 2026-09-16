package com.raspel.erp.service.envanter;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokMaliyetHareket;
import com.raspel.erp.repository.envanter.StokMaliyetHareketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Ağırlıklı ortalama maliyet (COGS) motoru.
 *
 * Girişte ortalama yeniden hesaplanır; çıkışta güncel ortalama tüketilir ve
 * satış anındaki birim maliyet anlık görüntü olarak döndürülür. Tüm giriş/çıkışlar
 * denetlenebilir bir maliyet defterine ({@code maliyet.stok_maliyet_hareket}) yazılır.
 *
 * Hata iş akışını bloklamaz: defter kaydı başarısız olsa bile maliyet hesaplanır.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MaliyetService {

    public static final String GIRIS = "GIRIS";
    public static final String CIKIS = "CIKIS";
    private static final int SCALE = 4;

    private final StokMaliyetHareketRepository stokMaliyetHareketRepository;

    /** Güncel ortalama maliyet; yoksa tedarikçi fiyatı, o da yoksa liste fiyatı. */
    public BigDecimal ortalamaMaliyet(Stok stok) {
        if (stok == null) return BigDecimal.ZERO;
        if (stok.getOrtalamaMaliyet() != null && stok.getOrtalamaMaliyet().signum() > 0) {
            return stok.getOrtalamaMaliyet();
        }
        if (stok.getTedarikciFiyat() != null && stok.getTedarikciFiyat().signum() > 0) {
            return stok.getTedarikciFiyat();
        }
        return stok.getFiyat() != null ? stok.getFiyat() : BigDecimal.ZERO;
    }

    /**
     * Stok girişini işler ve yeni ağırlıklı ortalamayı döndürür.
     *
     * @param eskiMiktar  girişten önceki stok miktarı
     * @param girenMiktar giren miktar (>0)
     * @param birimFiyat  giriş birim maliyeti (<=0 ise mevcut ortalama kullanılır)
     */
    public BigDecimal girisIsle(Stok stok, BigDecimal eskiMiktar, BigDecimal girenMiktar,
                                BigDecimal birimFiyat, Long sirketId, String kaynakTip, Long kaynakId) {
        if (stok == null || girenMiktar == null || girenMiktar.signum() <= 0) {
            return ortalamaMaliyet(stok);
        }
        BigDecimal fiyat = (birimFiyat != null && birimFiyat.signum() > 0) ? birimFiyat : ortalamaMaliyet(stok);
        BigDecimal em = eskiMiktar != null ? eskiMiktar : BigDecimal.ZERO;
        BigDecimal yeniOrt;
        if (em.signum() <= 0) {
            yeniOrt = fiyat;
        } else {
            BigDecimal toplam = em.add(girenMiktar);
            yeniOrt = em.multiply(ortalamaMaliyet(stok))
                    .add(girenMiktar.multiply(fiyat))
                    .divide(toplam, SCALE, RoundingMode.HALF_UP);
        }
        yeniOrt = yeniOrt.setScale(SCALE, RoundingMode.HALF_UP);
        stok.setOrtalamaMaliyet(yeniOrt);
        kaydet(stok, GIRIS, girenMiktar, fiyat, em.add(girenMiktar), yeniOrt, sirketId, kaynakTip, kaynakId);
        return yeniOrt;
    }

    /**
     * Stok çıkışını işler ve çıkışta kullanılan birim maliyeti döndürür.
     *
     * @param kalanMiktar çıkıştan sonraki stok miktarı (defter kaydı için)
     */
    public BigDecimal cikisIsle(Stok stok, BigDecimal cikanMiktar, BigDecimal kalanMiktar,
                                Long sirketId, String kaynakTip, Long kaynakId) {
        if (stok == null) return BigDecimal.ZERO;
        BigDecimal birim = ortalamaMaliyet(stok);
        BigDecimal miktar = cikanMiktar != null ? cikanMiktar : BigDecimal.ZERO;
        if (miktar.signum() <= 0) return birim;
        kaydet(stok, CIKIS, miktar, birim, kalanMiktar != null ? kalanMiktar : BigDecimal.ZERO,
                birim, sirketId, kaynakTip, kaynakId);
        return birim;
    }

    @Transactional(readOnly = true)
    public List<StokMaliyetHareket> hareketler(Long stokId) {
        return stokMaliyetHareketRepository.findByStokIdOrderByTarihAscIdAsc(stokId);
    }

    private void kaydet(Stok stok, String tur, BigDecimal miktar, BigDecimal birimMaliyet,
                        BigDecimal kalanMiktar, BigDecimal ortalama, Long sirketId,
                        String kaynakTip, Long kaynakId) {
        try {
            BigDecimal toplam = birimMaliyet.multiply(miktar).setScale(2, RoundingMode.HALF_UP);
            stokMaliyetHareketRepository.save(StokMaliyetHareket.builder()
                    .stokId(stok.getId()).sirketId(sirketId != null ? sirketId : stok.getSirketId())
                    .tarih(LocalDate.now()).tur(tur)
                    .miktar(miktar.setScale(SCALE, RoundingMode.HALF_UP))
                    .birimMaliyet(birimMaliyet.setScale(SCALE, RoundingMode.HALF_UP))
                    .toplamMaliyet(toplam)
                    .kalanMiktar(kalanMiktar.setScale(SCALE, RoundingMode.HALF_UP))
                    .ortalamaMaliyet(ortalama.setScale(SCALE, RoundingMode.HALF_UP))
                    .kaynakTip(kaynakTip).kaynakId(kaynakId)
                    .build());
        } catch (Exception e) {
            log.warn("Maliyet defteri kaydı oluşturulamadı (stok {}, tür {}): {}", stok.getId(), tur, e.getMessage());
        }
    }
}
