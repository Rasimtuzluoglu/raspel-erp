package com.raspel.erp.dto;

import com.raspel.erp.dto.envanter.ReceteKalemDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REDTEAM regresyon testleri — DTO doğrulama katmanı.
 *
 * <p>Her test, canlı sistemde kanıtlanmış bir saldırı payload'ının artık
 * <b>geçersiz</b> olduğunu doğrular. Kapsam:
 * <ul>
 *   <li>C1 — negatif iskonto ile kasa nakit yaratma (FINANCIAL)</li>
 *   <li>C3 — negatif miktar ile stok çoğaltma (STOCK)</li>
 *   <li>C4 — negatif reçete kalemi ile hammadde stoğu artırma (STOCK)</li>
 * </ul>
 *
 * <p>Bu testler yalnızca bean validation katmanını kanıtlar. Servis katmanındaki
 * ikinci savunma hattı için ilgili servis testlerine bakınız.
 */
@DisplayName("REDTEAM - DTO dogrulama regresyonlari")
class RedTeamDogrulamaTest {

    private static ValidatorFactory vf;
    private static Validator validator;

    @BeforeAll
    static void kur() {
        vf = Validation.buildDefaultValidatorFactory();
        validator = vf.getValidator();
    }

    @AfterAll
    static void kapat() {
        vf.close();
    }

    private static boolean gecerli(Object o) {
        return validator.validate(o).isEmpty();
    }

    // ---------------------------------------------------------------- C1
    @Nested
    @DisplayName("C1 - FaturaKalemDTO: negatif iskonto/KDV ile nakit yaratma")
    class C1NegatifIskonto {

        @Test
        @DisplayName("canli kanit: iskontoOrani=-500 ile 100 TL'lik kalem 600 TL'ye bloat edildi")
        void negatifIskontoluKalemReddedilir() {
            FaturaKalemDTO dto = FaturaKalemDTO.builder()
                    .stokId(1L)
                    .aciklama("Kalem")
                    .adet(BigDecimal.ONE)
                    .birimFiyat(new BigDecimal("100"))
                    .kdvOrani(new BigDecimal("20"))
                    .iskontoOrani(new BigDecimal("-500"))
                    .build();

            Set<?> ihlaller = validator.validate(dto);

            assertFalse(ihlaller.isEmpty(), "Negatif iskonto orani REDDEDILMELI");
        }

        @Test
        void negatifKdvReddedilir() {
            FaturaKalemDTO dto = FaturaKalemDTO.builder()
                    .stokId(1L)
                    .aciklama("Kalem")
                    .adet(BigDecimal.ONE)
                    .birimFiyat(new BigDecimal("100"))
                    .kdvOrani(new BigDecimal("-20"))
                    .iskontoOrani(BigDecimal.ZERO)
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Negatif KDV orani REDDEDILMELI");
        }

        @Test
        void yuzdenBiranUstuIskontoReddedilir() {
            FaturaKalemDTO dto = FaturaKalemDTO.builder()
                    .stokId(1L)
                    .aciklama("Kalem")
                    .adet(BigDecimal.ONE)
                    .birimFiyat(new BigDecimal("100"))
                    .kdvOrani(new BigDecimal("20"))
                    .iskontoOrani(new BigDecimal("150"))
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "%150 iskonto REDDEDILMELI");
        }

        @Test
        void negatifBirimFiyatReddedilir() {
            FaturaKalemDTO dto = FaturaKalemDTO.builder()
                    .stokId(1L)
                    .aciklama("Kalem")
                    .adet(BigDecimal.ONE)
                    .birimFiyat(new BigDecimal("-100"))
                    .kdvOrani(new BigDecimal("20"))
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Negatif birim fiyat REDDEDILMELI");
        }

        @Test
        void negatifMiktarReddedilir() {
            FaturaKalemDTO dto = FaturaKalemDTO.builder()
                    .stokId(1L)
                    .aciklama("Kalem")
                    .adet(new BigDecimal("-5"))
                    .birimFiyat(new BigDecimal("100"))
                    .kdvOrani(new BigDecimal("20"))
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Negatif miktar REDDEDILMELI");
        }

        @Test
        @DisplayName("normal satis hala gecerli")
        void normalKalemGecerli() {
            FaturaKalemDTO dto = FaturaKalemDTO.builder()
                    .stokId(1L)
                    .aciklama("Kalem")
                    .adet(new BigDecimal("2"))
                    .birimFiyat(new BigDecimal("100"))
                    .kdvOrani(new BigDecimal("20"))
                    .iskontoOrani(new BigDecimal("10"))
                    .build();

            assertTrue(gecerli(dto), "Normal fatura kalemi reddedilmemeli");
        }

        @Test
        @DisplayName("canli kanit: -50/1000 ve -100/1000 de gecmis -> bloat")
        void canliKanitOrnekleriReddedilir() {
            for (String oran : new String[]{"-50", "-100", "-500"}) {
                FaturaKalemDTO dto = FaturaKalemDTO.builder()
                        .stokId(1L)
                        .aciklama("Kalem")
                        .adet(BigDecimal.TEN)
                        .birimFiyat(new BigDecimal("100"))
                        .kdvOrani(new BigDecimal("20"))
                        .iskontoOrani(new BigDecimal(oran))
                        .build();
                assertFalse(validator.validate(dto).isEmpty(),
                        "Canli kanit orani " + oran + " reddedilmeliydi");
            }
        }
    }

    // ---------------------------------------------------------------- C3
    @Nested
    @DisplayName("C3 - StokHareketDTO: negatif miktar ile stok cogaltma")
    class C3NegatifStokHareketi {

        @Test
        @DisplayName("canli kanit: CIKIS -5000 ile stok 100 -> 5100 oldu")
        void negatifCikisReddedilir() {
            StokHareketDTO dto = StokHareketDTO.builder()
                    .tur("CIKIS")
                    .miktar(new BigDecimal("-5000"))
                    .hareketTarihi(java.time.LocalDate.now())
                    .build();

            assertFalse(validator.validate(dto).isEmpty(),
                    "Negatif stok hareketi REDDEDILMELI (stok cogaltma vektoru)");
        }

        @Test
        void negatifGirisReddedilir() {
            StokHareketDTO dto = StokHareketDTO.builder()
                    .tur("GIRIS")
                    .miktar(new BigDecimal("-1"))
                    .hareketTarihi(java.time.LocalDate.now())
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Negatif GIRIS REDDEDILMELI");
        }

        @Test
        void sifirMiktarReddedilir() {
            StokHareketDTO dto = StokHareketDTO.builder()
                    .tur("CIKIS")
                    .miktar(BigDecimal.ZERO)
                    .hareketTarihi(java.time.LocalDate.now())
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Sifir miktar REDDEDILMELI");
        }

        @Test
        void normalCikisGecerli() {
            StokHareketDTO dto = StokHareketDTO.builder()
                    .tur("CIKIS")
                    .miktar(new BigDecimal("5"))
                    .hareketTarihi(java.time.LocalDate.now())
                    .build();

            assertTrue(gecerli(dto), "Normal stok cikisi reddedilmemeli");
        }

        @Test
        @DisplayName("ondalık hassasiyet korunur (kuruş/gram olcekli stok)")
        void kucukOndalikliMiktarGecerli() {
            StokHareketDTO dto = StokHareketDTO.builder()
                    .tur("GIRIS")
                    .miktar(new BigDecimal("0.0001"))
                    .hareketTarihi(java.time.LocalDate.now())
                    .build();

            assertTrue(gecerli(dto), "0.0001 miktarli gecerli bir harekettir");
        }
    }

    // ---------------------------------------------------------------- C4
    @Nested
    @DisplayName("C4 - ReceteKalemDTO: negatif recete kalemi")
    class C4NegatifReceteKalemi {

        @Test
        @DisplayName("canli kanit: hammaddeId=2018, miktar=-10 -> HTTP 201, kalem.miktar=-10")
        void negatifReceteKalemiReddedilir() {
            ReceteKalemDTO dto = ReceteKalemDTO.builder()
                    .hammaddeId(2018L)
                    .miktar(new BigDecimal("-10"))
                    .build();

            assertFalse(validator.validate(dto).isEmpty(),
                    "Negatif recete kalemi REDDEDILMELI (hammadde stogu artirma vektoru)");
        }

        @Test
        void negatifFireReddedilir() {
            ReceteKalemDTO dto = ReceteKalemDTO.builder()
                    .hammaddeId(1L)
                    .miktar(BigDecimal.TEN)
                    .fireOrani(new BigDecimal("-5"))
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Negatif fire orani REDDEDILMELI");
        }

        @Test
        void yuzdenBiranUstuFireReddedilir() {
            ReceteKalemDTO dto = ReceteKalemDTO.builder()
                    .hammaddeId(1L)
                    .miktar(BigDecimal.TEN)
                    .fireOrani(new BigDecimal("120"))
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "%120 fire REDDEDILMELI");
        }

        @Test
        void nullHammaddeReddedilir() {
            ReceteKalemDTO dto = ReceteKalemDTO.builder()
                    .hammaddeId(null)
                    .miktar(BigDecimal.TEN)
                    .build();

            assertFalse(validator.validate(dto).isEmpty(), "Hammadde secimi zorunludur");
        }

        @Test
        void normalReceteKalemiGecerli() {
            ReceteKalemDTO dto = ReceteKalemDTO.builder()
                    .hammaddeId(1L)
                    .miktar(new BigDecimal("2.5"))
                    .fireOrani(new BigDecimal("3"))
                    .build();

            assertTrue(gecerli(dto), "Normal recete kalemi reddedilmemeli");
        }
    }
}
