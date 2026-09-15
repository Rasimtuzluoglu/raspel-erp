package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recete_kalem", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceteKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recete_id", nullable = false)
    private Long receteId;

    @Column(name = "hammadde_id", nullable = false)
    private Long hammaddeId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(length = 20)
    private String birim;

    @Column(name = "fire_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal fireOrani = BigDecimal.ZERO;
}
