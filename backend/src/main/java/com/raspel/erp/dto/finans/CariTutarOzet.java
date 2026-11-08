package com.raspel.erp.dto.finans;

import java.math.BigDecimal;

/** Cari 360 kartı için adet/tutar agregasyonu (sipariş, iade). */
public record CariTutarOzet(Long adet, BigDecimal tutar) {
}
