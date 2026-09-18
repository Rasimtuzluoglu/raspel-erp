package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pazarlama/satış kampanyası. Bütçe, harcama ve hedef kitle takibi yapılır;
 * lead'ler bir kampanyaya bağlanabilir.
 */
@Entity
@Table(name = "crm_kampanya", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmKampanya {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 250)
    private String ad;

    @Column(length = 50)
    private String tur;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String durum = "PLANLANDI";

    private LocalDate baslangic;

    private LocalDate bitis;

    @Column(precision = 19, scale = 2)
    private BigDecimal butce;

    @Column(precision = 19, scale = 2)
    private BigDecimal harcama;

    @Column(name = "hedef_kisi")
    private Integer hedefKisi;

    @Column(length = 1000)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "PLANLANDI";
    }
}
