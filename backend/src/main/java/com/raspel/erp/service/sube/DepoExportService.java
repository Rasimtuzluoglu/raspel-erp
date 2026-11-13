package com.raspel.erp.service.sube;

import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.dto.sube.DepoTransferDTO;
import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Depolar ekrani CSV disa aktarma.
 *
 * Kurallar: UTF-8 BOM, `;` ayirici, her hucre tirnakli, `= + - @` ile baslayan
 * hucrelere `'` oneki (formul enjeksiyonu).
 */
@Service
@RequiredArgsConstructor
public class DepoExportService {

    private final DepoService depoService;
    private final DepoTransferService depoTransferService;

    @Transactional(readOnly = true)
    public byte[] csv(String tur, Long sirketId, Long depoId, String q, Long subeId,
                      Boolean aktif, Boolean kritik, Boolean sifir, String durum) {
        String tip = tur == null ? "liste" : tur;
        StringBuilder sb = new StringBuilder("\uFEFF");
        switch (tip) {
            case "liste" -> {
                satir(sb, "Depo", "Şube", "Sorumlu", "Durum", "Kalem", "Kritik", "Sıfır",
                        "Stok Değeri", "Doluluk %");
                for (DepoDTO d : depoService.tumunuGetir(sirketId, q, subeId, aktif, kritik,
                        Pageable.unpaged()).getContent()) {
                    satir(sb, d.getAd(), d.getSubeAdi(), d.getSorumluKullaniciAd(),
                            Boolean.TRUE.equals(d.getAktif()) ? "Aktif" : "Pasif",
                            sayi(d.getKalemSayisi()), sayi(d.getKritikStokSayisi()),
                            sayi(d.getSifirStokSayisi()), para(d.getStokDegeri()),
                            d.getDolulukYuzde() != null ? d.getDolulukYuzde().toString() : "");
                }
            }
            case "stok" -> {
                if (depoId == null) {
                    throw new BusinessException("Stok dışa aktarımı için depo seçilmelidir");
                }
                satir(sb, "Stok Kodu", "Ürün", "Birim", "Miktar", "Min Stok", "Kritik", "Değer");
                for (DepoStokDTO s : depoService.stokSayfali(depoId, q, kritik, sifir,
                        Pageable.unpaged()).getContent()) {
                    satir(sb, s.getStokKodu(), s.getStokAd(), s.getBirim(),
                            para(s.getMiktar()), para(s.getMinMiktar()),
                            Boolean.TRUE.equals(s.getKritikMi()) ? "Evet" : "Hayır",
                            para(s.getDeger()));
                }
            }
            case "transfer" -> {
                satir(sb, "Kaynak", "Hedef", "Ürün", "Miktar", "Durum", "Talep Eden",
                        "Yaş (saat)", "SLA", "Red Sebebi");
                List<DepoTransferDTO> liste = depoTransferService.sayfali(sirketId, depoId, durum,
                        null, null, Pageable.unpaged()).getContent();
                for (DepoTransferDTO t : liste) {
                    satir(sb, t.getKaynakDepoAd(), t.getHedefDepoAd(), t.getStokAd(),
                            para(t.getMiktar()), t.getDurum(), t.getOlusturanKullaniciAd(),
                            sayi(t.getYasSaat()), t.getSlaDurumu(), t.getRedSebebi());
                }
            }
            default -> throw new BusinessException("Geçersiz dışa aktarma türü: " + tip);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void satir(StringBuilder sb, Object... hucreler) {
        for (int i = 0; i < hucreler.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(kacis(hucreler[i]));
        }
        sb.append('\n');
    }

    private static String kacis(Object deger) {
        String s = deger == null ? "" : String.valueOf(deger);
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String sayi(Number n) {
        return n == null ? "" : String.valueOf(n.longValue());
    }

    private static String para(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
