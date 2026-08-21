package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_config", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false, unique = true)
    private Long sirketId;

    @Column(nullable = false, length = 20)
    private String provider; // OPENAI, GOOGLE, ANTHROPIC

    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiKey;

    @Column(length = 50)
    private String model; // gpt-4o, gemini-2.5-flash, claude-sonnet-4

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        guncellemeTarihi = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
