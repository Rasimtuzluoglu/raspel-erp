package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.dto.sistem.PasskeyGirisRequest;
import com.raspel.erp.dto.sistem.PasskeyKayitRequest;
import com.raspel.erp.dto.sistem.PasskeySecenekDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.PasskeyRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.ClientDataType;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Gercek WebAuthn4J dogrulamasini sentetik bir authenticator ile test eder:
 * EC P-256 anahtar cifti, none attestation, imzali assertion.
 */
@ExtendWith(MockitoExtension.class)
class PasskeyServiceWebAuthnTest {

    @Mock private PasskeyRepository passkeyRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KullaniciService kullaniciService;
    @Mock private StringRedisTemplate redisTemplate;

    private PasskeyService service;
    private ObjectConverter objectConverter;
    private Kullanici kullanici;
    private KeyPair anahtarCifti;
    private byte[] credentialId;

    private static String b64(byte[] veri) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(veri);
    }

    @BeforeEach
    void kur() throws Exception {
        objectConverter = new ObjectConverter();
        WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
        service = new PasskeyService(passkeyRepository, kullaniciRepository, passwordEncoder,
                kullaniciService, redisTemplate, new ObjectMapper(), objectConverter, manager);
        ReflectionTestUtils.setField(service, "rpId", "localhost");
        ReflectionTestUtils.setField(service, "originsRaw", "http://localhost");

        kullanici = new Kullanici();
        kullanici.setId(1L);
        kullanici.setUsername("testuser");
        kullanici.setDisplayName("Test User");
        kullanici.setPassword("encoded");
        kullanici.setActive(true);
        kullanici.setSirketId(5L);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        anahtarCifti = generator.generateKeyPair();
        credentialId = new byte[32];
        new java.security.SecureRandom().nextBytes(credentialId);

        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(kullanici));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
    }

    @Test
    void kayitVeGiris_gercekImzaDogrulanir() throws Exception {
        // 1) Kayit secenekleri ve challenge
        when(passkeyRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(List.of());
        PasskeySecenekDTO kayitSecenek = service.kayitSecenekleri(1L, "pass");
        String challenge = (String) kayitSecenek.getPublicKey().get("challenge");

        // 2) Sentetik registration response
        byte[] rpIdHash = MessageDigest.getInstance("SHA-256")
                .digest("localhost".getBytes(StandardCharsets.UTF_8));
        EC2COSEKey cose = EC2COSEKey.create(anahtarCifti, COSEAlgorithmIdentifier.ES256);
        AttestedCredentialData acd = new AttestedCredentialData(AAGUID.ZERO, credentialId, cose);
        AuthenticatorData<com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput> authData =
                new AuthenticatorData<>(
                rpIdHash,
                (byte) (AuthenticatorData.BIT_UP | AuthenticatorData.BIT_UV | AuthenticatorData.BIT_AT),
                0L,
                acd);
        AttestationObject attestationObject = new AttestationObject(authData, new NoneAttestationStatement());
        byte[] attestationBytes = objectConverter.getCborConverter().writeValueAsBytes(attestationObject);

        CollectedClientData clientData = new CollectedClientData(
                ClientDataType.WEBAUTHN_CREATE,
                new DefaultChallenge(Base64.getUrlDecoder().decode(challenge)),
                new Origin("http://localhost"),
                false,
                null);
        byte[] clientDataBytes = objectConverter.getJsonConverter().writeValueAsBytes(clientData);

        Map<String, Object> credential = Map.of(
                "id", b64(credentialId),
                "rawId", b64(credentialId),
                "type", "public-key",
                "clientExtensionResults", Map.of(),
                "response", Map.of(
                        "attestationObject", b64(attestationBytes),
                        "clientDataJSON", b64(clientDataBytes),
                        "transports", List.of("internal")));

        when(passkeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var kayit = service.kaydet(1L, PasskeyKayitRequest.builder()
                .challengeId(kayitSecenek.getChallengeId())
                .etiket("Test Telefon")
                .credential(credential)
                .build());
        assertEquals("Test Telefon", kayit.getEtiket());

        // 3) Kaydedilen passkey ile giris
        var kaydedilen = org.mockito.ArgumentCaptor.forClass(com.raspel.erp.entity.sistem.Passkey.class);
        verify(passkeyRepository).save(kaydedilen.capture());
        when(passkeyRepository.findByCredentialId(any())).thenReturn(Optional.of(kaydedilen.getValue()));

        PasskeySecenekDTO girisSecenek = service.girisSecenekleri();
        String girisChallenge = (String) girisSecenek.getPublicKey().get("challenge");
        CollectedClientData girisClientData = new CollectedClientData(
                ClientDataType.WEBAUTHN_GET,
                new DefaultChallenge(Base64.getUrlDecoder().decode(girisChallenge)),
                new Origin("http://localhost"),
                false,
                null);
        byte[] girisClientDataBytes = objectConverter.getJsonConverter().writeValueAsBytes(girisClientData);
        byte[] girisClientDataHash = MessageDigest.getInstance("SHA-256").digest(girisClientDataBytes);

        byte[] girisAuthBytes = java.nio.ByteBuffer.allocate(37)
                .put(rpIdHash)
                .put((byte) (AuthenticatorData.BIT_UP | AuthenticatorData.BIT_UV))
                .putInt(1)
                .array();

        Signature imza = Signature.getInstance("SHA256withECDSA");
        imza.initSign(anahtarCifti.getPrivate());
        imza.update(girisAuthBytes);
        imza.update(girisClientDataHash);
        byte[] imzaBytes = imza.sign();

        Map<String, Object> assertion = Map.of(
                "id", b64(credentialId),
                "rawId", b64(credentialId),
                "type", "public-key",
                "clientExtensionResults", Map.of(),
                "response", Map.of(
                        "authenticatorData", b64(girisAuthBytes),
                        "clientDataJSON", b64(girisClientDataBytes),
                        "signature", b64(imzaBytes),
                        "userHandle", b64("1".getBytes(StandardCharsets.UTF_8))));

        LoginResponse beklenen = LoginResponse.builder().username("testuser").build();
        when(kullaniciService.passkeyGirisCevabi(eq(kullanici), any())).thenReturn(beklenen);

        LoginResponse sonuc = service.giris(PasskeyGirisRequest.builder()
                .challengeId(girisSecenek.getChallengeId())
                .credential(assertion)
                .build());

        assertSame(beklenen, sonuc);
        verify(kullaniciService).passkeyGirisCevabi(eq(kullanici), any());
    }
}
