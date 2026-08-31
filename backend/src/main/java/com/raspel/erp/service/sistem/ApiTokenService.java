package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.ApiTokenDTO;
import com.raspel.erp.entity.sistem.ApiToken;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.ApiTokenRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApiTokenService {

    private static final String TOKEN_PREFIX = "raspel_pat_";
    private final ApiTokenRepository apiTokenRepository;
    private final KullaniciRepository kullaniciRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<ApiTokenDTO> listele(Long kullaniciId) {
        return apiTokenRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(kullaniciId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    public ApiTokenDTO olustur(Long kullaniciId, String ad) {
        Kullanici kullanici = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));

        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        ApiToken apiToken = ApiToken.builder()
                .kullaniciId(kullaniciId)
                .ad(ad != null && !ad.isBlank() ? ad : "API Token")
                .tokenHash(sha256(token))
                .build();
        ApiToken saved = apiTokenRepository.save(apiToken);

        ApiTokenDTO dto = entityToDTO(saved);
        dto.setToken(token); // token yalnızca burada döner
        return dto;
    }

    public void sil(Long id, Long kullaniciId) {
        ApiToken apiToken = apiTokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Token", id));
        if (!apiToken.getKullaniciId().equals(kullaniciId)) {
            throw new ResourceNotFoundException("API Token", id);
        }
        apiTokenRepository.deleteById(id);
    }

    /** Token hash'inden kullanıcıyı çözer. API isteklerinde kimlik doğrulama için kullanılır. */
    @Transactional(readOnly = true)
    public Kullanici tokenIleKullaniciBul(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) return null;
        return apiTokenRepository.findByTokenHash(sha256(token))
                .map(t -> kullaniciRepository.findById(t.getKullaniciId()).orElse(null))
                .orElse(null);
    }

    public void sonKullanimGuncelle(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) return;
        apiTokenRepository.findByTokenHash(sha256(token)).ifPresent(t -> {
            t.setSonKullanim(LocalDateTime.now());
            apiTokenRepository.save(t);
        });
    }

    private String sha256(String deger) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deger.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Token hash hesaplanamadı", e);
        }
    }

    private ApiTokenDTO entityToDTO(ApiToken t) {
        return ApiTokenDTO.builder()
                .id(t.getId())
                .kullaniciId(t.getKullaniciId())
                .ad(t.getAd())
                .sonKullanim(t.getSonKullanim())
                .olusturmaTarihi(t.getOlusturmaTarihi())
                .build();
    }
}
