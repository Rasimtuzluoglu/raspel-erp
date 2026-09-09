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
}
