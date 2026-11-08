package com.raspel.erp.dto.finans;

import java.math.BigDecimal;

/** Cari 360 kartı için fatura agregasyonu. */
public record CariFaturaOzet(Long adet, BigDecimal tutar, BigDecimal kalan) {
}
