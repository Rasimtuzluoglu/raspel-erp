package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.ik.Personel;

@Entity
@Table(name = "rol", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad", nullable = false, unique = true, length = 50)
    private String ad; // ADMIN, MUHASEBE, SATIS, DEPO, PERSONEL

    @Column(name = "aciklama", length = 150)
    private String aciklama;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rol_yetki",
        schema = "sistem",
        joinColumns = @JoinColumn(name = "rol_id"),
        inverseJoinColumns = @JoinColumn(name = "yetki_id")
    )
    @Builder.Default
    private Set<Yetki> yetkiler = new HashSet<>();
}