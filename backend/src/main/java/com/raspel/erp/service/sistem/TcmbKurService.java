package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.DovizKuruRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TcmbKurService {

    private final DovizKuruRepository dovizKuruRepository;

    @Scheduled(cron = "0 0 9,12,16 * * ?")
    @org.springframework.transaction.annotation.Transactional
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "tcmbKurGuncelle", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void tcmbKurlariniGuncelle() {
        log.info("TCMB Güncel Döviz Kurları çekiliyor...");
        Map<String, BigDecimal[]> rates = new HashMap<>();

        try {
            URL url = new URL("https://www.tcmb.gov.tr/kurlar/today.xml");
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36");
            conn.setRequestProperty("Accept", "application/xml, text/xml, */*");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // XXE sertlestirme: DOCTYPE/external entity'leri kapat, secure processing zorla.
            dbFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbFactory.setXIncludeAware(false);
            dbFactory.setExpandEntityReferences(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            try (InputStream is = conn.getInputStream()) {
                doc = dBuilder.parse(is);
            }
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("Currency");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Element element = (Element) nList.item(temp);
                String code = element.getAttribute("CurrencyCode");
                if (Arrays.asList("USD", "EUR", "GBP", "SAR").contains(code)) {
                    String alisStr = getTagValue("ForexBuying", element);
                    String satisStr = getTagValue("ForexSelling", element);
                    if (alisStr != null && satisStr != null && !alisStr.isBlank() && !satisStr.isBlank()) {
                        BigDecimal alis = new BigDecimal(alisStr.trim());
                        BigDecimal satis = new BigDecimal(satisStr.trim());
                        rates.put(code, new BigDecimal[]{alis, satis});
                    }
                }
            }
        } catch (Exception e) {
            log.warn("TCMB servisine erişilemedi: {}", e.getMessage());
        }

        // TCMB'den veri alınamadıysa hiçbir kur KAYDEDİLMEZ; eski kurlar korunur.
        // Sabit fallback değerleri DB'ye yazmak yanlış kurla işlem yapılmasına yol açar.
        if (rates.isEmpty()) {
            log.warn("TCMB kurları alınamadı; mevcut kurlar korunuyor (yeni veri yazılmadı).");
            return;
        }

        // Calculate Gram Gold (GAU) approx based on USD rate
        BigDecimal usdSatis = rates.containsKey("USD") ? rates.get("USD")[1] : null;
        if (usdSatis == null) {
            log.warn("TCMB yanitinda USD kuru yok; altin (GAU) kuru hesaplanamadi.");
        } else {
            BigDecimal goldGramAlis = usdSatis.multiply(new BigDecimal("89.50")).setScale(4, RoundingMode.HALF_UP);
            BigDecimal goldGramSatis = usdSatis.multiply(new BigDecimal("90.50")).setScale(4, RoundingMode.HALF_UP);
            rates.put("GAU", new BigDecimal[]{goldGramAlis, goldGramSatis});
        }

        // Bitcoin (BTC) kuru kripto borsasından çekilir (TCMB'de yer almaz)
        BigDecimal btcFiyat = btcFiyatiGetir();
        if (btcFiyat != null && btcFiyat.compareTo(BigDecimal.ZERO) > 0) {
            rates.put("BTC", new BigDecimal[]{btcFiyat, btcFiyat});
        }

        Map<String, String[]> metadata = Map.of(
            "USD", new String[]{"ABD Doları", "$"},
            "EUR", new String[]{"Euro", "€"},
            "GBP", new String[]{"İngiliz Sterlini", "£"},
            "SAR", new String[]{"Suudi Arabistan Riyali", "﷼"},
            "GAU", new String[]{"Gram Altın", "GAU"},
            "BTC", new String[]{"Bitcoin", "₿"}
        );

        rates.forEach((kod, vals) -> {
            String[] meta = metadata.get(kod);
            DovizKuru kuru = dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc(kod)
                .orElse(DovizKuru.builder().dovizKodu(kod).tarih(java.time.LocalDate.now()).build());

            kuru.setDovizAdi(meta[0]);
            kuru.setAlisKuru(vals[0]);
            kuru.setSatisKuru(vals[1]);
            kuru.setEfektifAlis(vals[0]);
            kuru.setEfektifSatis(vals[1]);
            kuru.setTarih(java.time.LocalDate.now());

            dovizKuruRepository.save(kuru);
        });

        log.info("Döviz kurları başarıyla güncellendi: {}", rates.keySet());
    }

    public List<DovizKuru> tumKurlariGetir() {
        // Bugünün kuru yoksa TCMB'den tazelenir; liste yalnızca bir kez çekilir.
        if (dovizKuruRepository.countByTarih(LocalDate.now()) == 0) {
            tcmbKurlariniGuncelle();
        }
        return dovizKuruRepository.findAll();
    }

    public BigDecimal cevir(BigDecimal tutar, String kaynakKod, String hedefKod) {
        if (tutar == null || tutar.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        String kaynak = normalizeKod(kaynakKod);
        String hedef = normalizeKod(hedefKod);
        if (kaynak.equals(hedef)) return tutar;

        // Kur kaydı hiç yoksa tek seferlik yenileme denenir. `countByTarih` TÜM
        // para birimlerinin satır sayısını döndürdüğü için burada yalnızca
        // kaynak/ hedef kodlara bakmak gerekir: sadece USD kaydı varken CHF
        // faturası "bugünün kurları var" diye geçiyor ve 1:1 kabul ediliyordu.
        // TRY taban birim olduğu için sorgulanmaz (kur her zaman 1).
        boolean kaynakKurVar = "TRY".equals(kaynak) || kaynakKur(kaynak) != null;
        boolean hedefKurVar = "TRY".equals(hedef) || kaynakKur(hedef) != null;
        if (!kaynakKurVar || !hedefKurVar) {
            tcmbKurlariniGuncelle();
        }

        BigDecimal kaynakRateInTry = kurTry(kaynak);
        BigDecimal hedefRateInTry = kurTry(hedef);
        // Artık null gelemez; yine de savunma olarak guard bırakılıyor.
        if (hedefRateInTry.signum() == 0) {
            throw new BusinessException(hedef + " için geçerli bir kur bulunamadı; işlem iptal edildi");
        }

        BigDecimal tryValue = tutar.multiply(kaynakRateInTry);
        return tryValue.divide(hedefRateInTry, 4, RoundingMode.HALF_UP);
    }

    /** Para birimi kodunu büyük harfe çevirip boşsa hata verir. */
    private String normalizeKod(String kod) {
        if (kod == null || kod.isBlank()) {
            throw new BusinessException("Para birimi kodu zorunludur");
        }
        return kod.trim().toUpperCase(Locale.ROOT);
    }

    /** Kod için daha önce yüklenmiş bir kur satırı var mı (tarih filtresiz). */
    private DovizKuru kaynakKur(String kod) {
        return dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc(kod).orElse(null);
    }

    /**
     * Bir doviz kodunun TRY karsiligi (en yeni tarihli kayit).
     *
     * <p><b>Kritik:</b> eski uygulama kayit yoksa sessizce {@code 1} donuyordu.
     * {@code Fatura.paraBirimi} serbest metin oldugu icin kur tablosunda olmayan
     * bir kod ("CHF", "JPY", hatta yazim hatasi "USDD") faturanin TL karsiligini
     * 1:1 aliyordu: 1.000 CHF kayitta 1.000 TL olarak gorunuyordu. Ne exception
     * ne log ne uyari uretiliyordu. Artik bilinmeyen/kursuz para birimi
     * islemi durdurur.
     */
    private BigDecimal kurTry(String kod) {
        if (kod == null || kod.isBlank() || kod.equals("TRY")) return BigDecimal.ONE;
        return dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc(kod)
                .map(k -> k.getSatisKuru())
                .orElseThrow(() -> new BusinessException(
                        kod + " için kur kaydı bulunamadı. Döviz kurlarını güncelleyip tekrar deneyin."));
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /** Bitcoin (BTC) fiyatını TRY cinsinden kripto borsası API'sinden çeker. */
    private BigDecimal btcFiyatiGetir() {
        String[] kaynaklar = {
            "https://api.binance.com/api/v3/ticker/price?symbol=BTCTRY",
            "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=try"
        };
        for (String urlStr : kaynaklar) {
            try {
                URL url = new URL(urlStr);
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                try (InputStream is = conn.getInputStream()) {
                    String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    BigDecimal fiyat = parseBtcFiyat(json);
                    if (fiyat != null && fiyat.compareTo(BigDecimal.ZERO) > 0) {
                        return fiyat;
                    }
                }
            } catch (Exception e) {
                log.warn("BTC kuru alınamadı ({}): {}", urlStr, e.getMessage());
            }
        }
        return null;
    }

    /** Binance ("price":"...") veya CoinGecko ("try":...) JSON yanıtından BTC fiyatını ayıklar. */
    private BigDecimal parseBtcFiyat(String json) {
        java.util.regex.Matcher binance = java.util.regex.Pattern
                .compile("\"price\"\\s*:\\s*\"([0-9.]+)\"").matcher(json);
        if (binance.find()) {
            return new BigDecimal(binance.group(1));
        }
        java.util.regex.Matcher coingecko = java.util.regex.Pattern
                .compile("\"try\"\\s*:\\s*([0-9.]+)").matcher(json);
        if (coingecko.find()) {
            return new BigDecimal(coingecko.group(1));
        }
        return null;
    }
}
