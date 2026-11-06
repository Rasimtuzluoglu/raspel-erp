package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.*;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Passkey;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.PasskeyRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Passkey (WebAuthn) kaydi, listeleme ve parolasiz girisi.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PasskeyService {

    private static final String REG_PREFIX = "passkey:reg:";
    private static final String AUTH_PREFIX = "passkey:auth:";
    private static final Duration REG_TTL = Duration.ofMinutes(5);
    private static final Duration AUTH_TTL = Duration.ofMinutes(2);
    private static final int MAX_PASSKEY = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasskeyRepository passkeyRepository;
    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder passwordEncoder;
    private final KullaniciService kullaniciService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectConverter objectConverter;
    private final WebAuthnManager webAuthnManager;

    @Value("${app.webauthn.rp-id:localhost}")
    private String rpId;

    @Value("${app.webauthn.origins:http://localhost,http://localhost:5173,http://127.0.0.1:5173}")
    private String originsRaw;

    private final Map<String, ChallengeKayit> bellekChallenge = new ConcurrentHashMap<>();

    record ChallengeKayit(Long kullaniciId, String challenge, long sonGecerlilik) {
    }

    // ------------------------------------------------------------------
    // Kayit
    // ------------------------------------------------------------------

    public PasskeySecenekDTO kayitSecenekleri(Long kullaniciId, String password) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        if (password == null || password.isBlank()
                || !passwordEncoder.matches(password, k.getPassword())) {
            throw new BusinessException("Parola doğrulanamadı");
        }
        List<Passkey> mevcut = passkeyRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(kullaniciId);
        if (mevcut.size() >= MAX_PASSKEY) {
            throw new BusinessException("En fazla " + MAX_PASSKEY + " passkey kaydedebilirsiniz");
        }

        byte[] challenge = rastgele(32);
        String challengeId = UUID.randomUUID().toString();
        challengeKaydet(REG_PREFIX, challengeId,
                new ChallengeKayit(kullaniciId, b64(challenge), System.currentTimeMillis() + REG_TTL.toMillis()),
                REG_TTL);

        Map<String, Object> publicKey = new LinkedHashMap<>();
        publicKey.put("challenge", b64(challenge));
        publicKey.put("rp", Map.of("name", "RasPel ERP", "id", rpId));
        publicKey.put("user", Map.of(
                "id", b64(userHandle(k)),
                "name", k.getUsername(),
                "displayName", k.getDisplayName() != null ? k.getDisplayName() : k.getUsername()));
        publicKey.put("pubKeyCredParams", List.of(
                Map.of("type", "public-key", "alg", -7),
                Map.of("type", "public-key", "alg", -257)));
        publicKey.put("timeout", 60_000);
        publicKey.put("attestation", "none");
        publicKey.put("authenticatorSelection", Map.of(
                "authenticatorAttachment", "platform",
                "residentKey", "required",
                "requireResidentKey", true,
                "userVerification", "required"));
        if (!mevcut.isEmpty()) {
            publicKey.put("excludeCredentials", mevcut.stream()
                    .map(p -> Map.of(
                            "type", "public-key",
                            "id", p.getCredentialId(),
                            "transports", transportListesi(p.getTransports())))
                    .collect(Collectors.toList()));
        }

        return PasskeySecenekDTO.builder().challengeId(challengeId).publicKey(publicKey).build();
    }

    public PasskeyDTO kaydet(Long kullaniciId, PasskeyKayitRequest req) {
        if (req == null || req.getChallengeId() == null || req.getChallengeId().isBlank()
                || req.getCredential() == null) {
            throw new BusinessException("Passkey kaydı için eksik bilgi");
        }
        ChallengeKayit kayit = challengeOku(REG_PREFIX, req.getChallengeId());
        if (kayit == null || !Objects.equals(kayit.kullaniciId(), kullaniciId)) {
            throw new BusinessException("Passkey kayıt oturumu geçersiz veya süresi doldu");
        }
        challengeSil(REG_PREFIX, req.getChallengeId());

        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));

        try {
            String json = objectMapper.writeValueAsString(req.getCredential());
            RegistrationData data = webAuthnManager.parseRegistrationResponseJSON(json);
            RegistrationParameters params = new RegistrationParameters(
                    serverProperty(kayit.challenge()),
                    List.of(
                            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
                                    COSEAlgorithmIdentifier.ES256),
                            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
                                    COSEAlgorithmIdentifier.RS256)),
                    true,
                    true);
            webAuthnManager.verify(data, params);

            AttestedCredentialData acd = data.getAttestationObject()
                    .getAuthenticatorData().getAttestedCredentialData();
            if (acd == null) {
                throw new BusinessException("Passkey kimlik bilgisi okunamadı");
            }
            String credentialId = b64(acd.getCredentialId());
            if (passkeyRepository.findByCredentialId(credentialId).isPresent()) {
                throw new BusinessException("Bu passkey zaten kayıtlı");
            }

            String etiket = req.getEtiket() == null || req.getEtiket().isBlank()
                    ? "Passkey"
                    : req.getEtiket().trim();
            if (etiket.length() > 100) etiket = etiket.substring(0, 100);

            Passkey p = Passkey.builder()
                    .kullaniciId(kullaniciId)
                    .credentialId(credentialId)
                    .etiket(etiket)
                    .attestedCredentialData(b64(new AttestedCredentialDataConverter(objectConverter).convert(acd)))
                    .counter(data.getAttestationObject().getAuthenticatorData().getSignCount())
                    .uvInitialized(data.getAttestationObject().getAuthenticatorData().isFlagUV())
                    .backupEligible(data.getAttestationObject().getAuthenticatorData().isFlagBE())
                    .backedUp(data.getAttestationObject().getAuthenticatorData().isFlagBS())
                    .transports(data.getTransports() == null ? null : data.getTransports().stream()
                            .map(AuthenticatorTransport::getValue).sorted().collect(Collectors.joining(",")))
                    .userHandle(b64(userHandle(k)))
                    .build();
            return dto(passkeyRepository.save(p));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Passkey kaydı doğrulanamadı - kullanıcı: {}, neden: {}", k.getUsername(), e.getMessage());
            throw new BusinessException("Passkey doğrulanamadı, lütfen tekrar deneyin");
        }
    }

    @Transactional(readOnly = true)
    public List<PasskeyDTO> listele(Long kullaniciId) {
        return passkeyRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(kullaniciId)
                .stream().map(this::dto).collect(Collectors.toList());
    }

    public void sil(Long kullaniciId, Long id) {
        Passkey p = passkeyRepository.findByIdAndKullaniciId(id, kullaniciId)
                .orElseThrow(() -> new BusinessException("Passkey bulunamadı"));
        passkeyRepository.delete(p);
    }

    public void adminSil(Long kullaniciId) {
        passkeyRepository.deleteByKullaniciId(kullaniciId);
    }

    // ------------------------------------------------------------------
    // Parolasiz giris
    // ------------------------------------------------------------------

    public PasskeySecenekDTO girisSecenekleri() {
        byte[] challenge = rastgele(32);
        String challengeId = UUID.randomUUID().toString();
        challengeKaydet(AUTH_PREFIX, challengeId,
                new ChallengeKayit(null, b64(challenge), System.currentTimeMillis() + AUTH_TTL.toMillis()),
                AUTH_TTL);

        Map<String, Object> publicKey = new LinkedHashMap<>();
        publicKey.put("challenge", b64(challenge));
        publicKey.put("timeout", 60_000);
        publicKey.put("rpId", rpId);
        publicKey.put("userVerification", "required");
        return PasskeySecenekDTO.builder().challengeId(challengeId).publicKey(publicKey).build();
    }

    public LoginResponse giris(PasskeyGirisRequest req) {
        if (req == null || req.getChallengeId() == null || req.getChallengeId().isBlank()
                || req.getCredential() == null) {
            throw new BusinessException("Passkey girişi için eksik bilgi");
        }
        ChallengeKayit kayit = challengeOku(AUTH_PREFIX, req.getChallengeId());
        if (kayit == null) {
            throw new BusinessException("Passkey giriş oturumu geçersiz veya süresi doldu");
        }
        challengeSil(AUTH_PREFIX, req.getChallengeId());

        try {
            String json = objectMapper.writeValueAsString(req.getCredential());
            AuthenticationData data = webAuthnManager.parseAuthenticationResponseJSON(json);
            String credentialId = b64(data.getCredentialId());
            Passkey p = passkeyRepository.findByCredentialId(credentialId)
                    .orElseThrow(() -> new BusinessException("Passkey doğrulanamadı"));
            Kullanici k = kullaniciRepository.findById(p.getKullaniciId())
                    .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
            if (!Boolean.TRUE.equals(k.getActive())) {
                throw new BusinessException("Bu kullanıcı aktif değil");
            }
            if (p.getUserHandle() != null && data.getUserHandle() != null
                    && !p.getUserHandle().equals(b64(data.getUserHandle()))) {
                throw new BusinessException("Passkey doğrulanamadı");
            }

            AttestedCredentialData acd = new AttestedCredentialDataConverter(objectConverter)
                    .convert(Base64.getUrlDecoder().decode(p.getAttestedCredentialData()));
            CredentialRecord record = new CredentialRecordImpl(
                    null,
                    Boolean.TRUE.equals(p.getUvInitialized()),
                    p.getBackupEligible(),
                    p.getBackedUp(),
                    p.getCounter() != null ? p.getCounter() : 0L,
                    acd,
                    null,
                    null,
                    null,
                    transportSet(p.getTransports()));

            AuthenticationParameters params = new AuthenticationParameters(
                    serverProperty(kayit.challenge()), record, null, true, true);
            webAuthnManager.verify(data, params);

            long yeniCounter = data.getAuthenticatorData().getSignCount();
            long eskiCounter = p.getCounter() != null ? p.getCounter() : 0L;
            if (yeniCounter > 0 && eskiCounter > 0 && yeniCounter <= eskiCounter) {
                throw new BusinessException("Passkey sayaç doğrulaması başarısız");
            }
            p.setCounter(Math.max(yeniCounter, eskiCounter));
            p.setSonKullanmaTarihi(LocalDateTime.now());
            passkeyRepository.save(p);

            return kullaniciService.passkeyGirisCevabi(k, req.getSirketKodu());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Passkey girişi doğrulanamadı: {}", e.getMessage());
            throw new BusinessException("Passkey doğrulanamadı, lütfen tekrar deneyin");
        }
    }

    // ------------------------------------------------------------------
    // Yardimcilar
    // ------------------------------------------------------------------

    private ServerProperty serverProperty(String challengeB64) {
        Set<Origin> origins = Arrays.stream(originsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Origin::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (origins.isEmpty()) {
            origins = Set.of(new Origin("http://localhost"));
        }
        return new ServerProperty(origins, rpId, new DefaultChallenge(Base64.getUrlDecoder().decode(challengeB64)));
    }

    private byte[] userHandle(Kullanici k) {
        return String.valueOf(k.getId()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Set<AuthenticatorTransport> transportSet(String transports) {
        if (transports == null || transports.isBlank()) return Set.of();
        return Arrays.stream(transports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AuthenticatorTransport::create)
                .collect(Collectors.toSet());
    }

    private List<String> transportListesi(String transports) {
        if (transports == null || transports.isBlank()) return List.of();
        return Arrays.stream(transports.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private PasskeyDTO dto(Passkey p) {
        return PasskeyDTO.builder()
                .id(p.getId())
                .etiket(p.getEtiket())
                .transports(p.getTransports())
                .olusturmaTarihi(p.getOlusturmaTarihi())
                .sonKullanmaTarihi(p.getSonKullanmaTarihi())
                .build();
    }

    private byte[] rastgele(int boyut) {
        byte[] b = new byte[boyut];
        RANDOM.nextBytes(b);
        return b;
    }

    private String b64(byte[] veri) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(veri);
    }

    private void challengeKaydet(String prefix, String id, ChallengeKayit kayit, Duration ttl) {
        bellekChallenge.put(prefix + id, kayit);
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(prefix + id,
                        objectMapper.writeValueAsString(kayit), ttl);
            }
        } catch (Exception e) {
            log.warn("Passkey challenge Redis'e yazılamadı, bellekte tutulacak: {}", e.getMessage());
        }
    }

    private ChallengeKayit challengeOku(String prefix, String id) {
        try {
            if (redisTemplate != null) {
                String json = redisTemplate.opsForValue().get(prefix + id);
                if (json != null) {
                    ChallengeKayit kayit = objectMapper.readValue(json, ChallengeKayit.class);
                    if (kayit.sonGecerlilik() < System.currentTimeMillis()) return null;
                    return kayit;
                }
            }
        } catch (Exception e) {
            log.warn("Passkey challenge Redis'ten okunamadı: {}", e.getMessage());
        }
        ChallengeKayit kayit = bellekChallenge.get(prefix + id);
        if (kayit == null || kayit.sonGecerlilik() < System.currentTimeMillis()) return null;
        return kayit;
    }

    private void challengeSil(String prefix, String id) {
        bellekChallenge.remove(prefix + id);
        try {
            if (redisTemplate != null) redisTemplate.delete(prefix + id);
        } catch (Exception e) {
            // yoksay
        }
    }
}
