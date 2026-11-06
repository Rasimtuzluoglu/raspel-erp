package com.raspel.erp.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebAuthn (passkey) dogrulama altyapisi.
 *
 * <p>Non-strict manager kullanilir: attestation trust chain dogrulanmaz,
 * ancak challenge, origin, rpId, user verification ve imza dogrulamalari
 * WebAuthn4J tarafindan yapilir. Passkey kaydi yalnizca oturum acmis ve
 * parolasini dogrulamis kullaniciya aciktir.
 */
@Configuration
public class WebAuthnConfig {

    @Bean
    public ObjectConverter objectConverter() {
        return new ObjectConverter();
    }

    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {
        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }
}
