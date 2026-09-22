package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "ip_whitelist", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpWhitelist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "ip_adresi", nullable = false, length = 100)
    private String ipAdresi;

    @Column(name = "aciklama", length = 255)
    private String aciklama;

    @Column(name = "durum", length = 20)
    private String durum;

    @Column(name = "ekleme_tarihi")
    private LocalDate eklemeTarihi;
}
