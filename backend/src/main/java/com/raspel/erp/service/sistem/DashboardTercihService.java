package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.DashboardTercihDTO;
import com.raspel.erp.entity.sistem.DashboardTercih;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.DashboardTercihRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Anasayfa düzeni (tercih) okuma/yazma.
 *
 * <p>Kurallar:
 * <ul>
 *   <li>Yalnızca kullanıcının KENDİ kaydı okunur/yazılır (kullaniciId).</li>
 *   <li>Tenant izolasyonu: kayıt yalnızca aynı {@code sirketId} için döner.</li>
 *   <li>{@code duzen} geçerli bir JSON <b>object</b> ve en fazla 20 KB olmalıdır.</li>
 *   <li>Tercih cache'lenmez (kullanıcıya özel, sık değişen veri).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardTercihService {

    /** Düzen JSON'u için üst sınır (bayt). */
    static final int MAKS_BOYUT_BAYT = 20 * 1024;

    private final DashboardTercihRepository dashboardTercihRepository;
    private final ObjectMapper objectMapper;

    /**
     * Kullanıcının düzeni. Kayıt yoksa (veya başka şirkete aitse)
     * {@code duzen = null} döner.
     */
    @Transactional(readOnly = true)
    public DashboardTercihDTO tercihGetir(Long kullaniciId, Long sirketId) {
        dogrula(kullaniciId, sirketId);
        return dashboardTercihRepository.findByKullaniciIdAndSirketId(kullaniciId, sirketId)
                .map(t -> DashboardTercihDTO.builder()
                        .duzen(jsonOkuma(t.getDuzen()))
                        .guncellemeTarihi(t.getGuncellemeTarihi())
                        .build())
                .orElseGet(() -> DashboardTercihDTO.builder().duzen(null).build());
    }

    /** Upsert: kullanıcının düzenini kaydeder (yoksa oluşturur). */
    @Transactional
    public DashboardTercihDTO tercihKaydet(Long kullaniciId, Long sirketId, DashboardTercihDTO dto) {
        dogrula(kullaniciId, sirketId);
        if (dto == null || dto.getDuzen() == null) {
            throw new BusinessException("Düzen bilgisi boş olamaz");
        }
        String json = jsonDogrula(dto.getDuzen());

        DashboardTercih tercih = dashboardTercihRepository.findByKullaniciId(kullaniciId)
                .orElseGet(() -> DashboardTercih.builder()
                        .kullaniciId(kullaniciId)
                        .sirketId(sirketId)
                        .build());

        // Tenant kontrolü: başka şirkette kayıtlı düzen bu istek üzerinden değiştirilemez.
        if (tercih.getId() != null && !sirketId.equals(tercih.getSirketId())) {
            throw new BusinessException("Bu düzen başka bir şirkete ait; bu şirkette kaydedilemez");
        }

        tercih.setSirketId(sirketId);
        tercih.setDuzen(json);
        tercih.setGuncellemeTarihi(LocalDateTime.now());
        DashboardTercih kaydedilen = dashboardTercihRepository.save(tercih);

        return DashboardTercihDTO.builder()
                .duzen(jsonOkuma(kaydedilen.getDuzen()))
                .guncellemeTarihi(kaydedilen.getGuncellemeTarihi())
                .build();
    }

    private void dogrula(Long kullaniciId, Long sirketId) {
        if (kullaniciId == null) {
            throw new BusinessException("Kullanıcı bilgisi bulunamadı");
        }
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi bulunamadı");
        }
    }

    /**
     * Gövdeyi yeniden serileştirir, JSON object olduğunu doğrular ve boyut
     * sınırını uygular. Böylece istemciden gelen her ne olursa olsun DB'ye
     * yalnızca normalize edilmiş bir JSON object yazılır.
     */
    private String jsonDogrula(Map<String, Object> duzen) {
        String json;
        try {
            json = objectMapper.writeValueAsString(duzen);
        } catch (JsonProcessingException e) {
            log.warn("Dashboard düzeni serileştirilemedi: {}", e.getMessage());
            throw new BusinessException("Düzen geçerli bir JSON nesnesi değil");
        }

        int boyut = json.getBytes(StandardCharsets.UTF_8).length;
        if (boyut > MAKS_BOYUT_BAYT) {
            throw new BusinessException("Düzen en fazla 20 KB olabilir");
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new BusinessException("Düzen bir JSON nesnesi olmalıdır");
            }
        } catch (JsonProcessingException e) {
            log.warn("Dashboard düzeni ayrıştırılamadı: {}", e.getMessage());
            throw new BusinessException("Düzen geçerli bir JSON nesnesi değil");
        }
        return json;
    }

    private Map<String, Object> jsonOkuma(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            // Bozulmuş kayıt: kullanıcıyı kilitlemek yerine varsayılana düş.
            log.warn("Dashboard düzeni okunamadı (varsayılana dönülecek): {}", e.getMessage());
            return null;
        }
    }
}
