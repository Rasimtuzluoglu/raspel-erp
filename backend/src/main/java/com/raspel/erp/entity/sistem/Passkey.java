package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "passkey", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passkey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "credential_id", nullable = false, unique = true, length = 255)
    private String credentialId;

    @Column(nullable = false, length = 100)
    private String etiket;

    /** AttestedCredentialData'nin base64url kodlanmis CBOR hali. */
    @Column(name = "attested_credential_data", nullable = false, columnDefinition = "TEXT")
    private String attestedCredentialData;

    @Column(nullable = false)
    private Long counter;

    @Column(name = "uv_initialized", nullable = false)
    private Boolean uvInitialized;

    @Column(name = "backup_eligible")
    private Boolean backupEligible;

    @Column(name = "backed_up")
    private Boolean backedUp;

    @Column(length = 255)
    private String transports;

    @Column(name = "user_handle", length = 255)
    private String userHandle;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "son_kullanma_tarihi")
    private LocalDateTime sonKullanmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
