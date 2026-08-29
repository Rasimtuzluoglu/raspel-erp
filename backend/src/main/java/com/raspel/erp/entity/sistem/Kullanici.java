package com.raspel.erp.entity.sistem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "kullanici", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "sirket_id")
    private Long sirketId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "kullanici_sirket",
        schema = "sistem",
        joinColumns = @JoinColumn(name = "kullanici_id"),
        inverseJoinColumns = @JoinColumn(name = "sirket_id")
    )
    @Builder.Default
    @JsonIgnore
    private Set<Sirket> sirketler = new HashSet<>();

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 100)
    @JsonIgnore
    private String twoFactorSecret;

    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Long tokenVersion = 0L;

    @Column(name = "bildirim_tercihleri", columnDefinition = "TEXT")
    private String bildirimTercihleri;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (active == null) active = true;
        if (role == null) role = "USER";
        if (tokenVersion == null) tokenVersion = 0L;
    }
}