package com.raspel.erp.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class SayfalamaUtilTest {

    @Test
    void parametresizIstekSayfaliDegildir() {
        assertThat(SayfalamaUtil.sayfaliMi(null, null)).isFalse();
        assertThat(SayfalamaUtil.sayfaliMi(0, null)).isTrue();
        assertThat(SayfalamaUtil.sayfaliMi(null, 10)).isTrue();
    }

    @Test
    void varsayilanDegerlerUygulanir() {
        assertThat(SayfalamaUtil.coz(null, null, Sort.unsorted()))
                .isEqualTo(PageRequest.of(0, SayfalamaUtil.VARSAYILAN_BOYUT));
    }

    @Test
    void negatifVeAsiriDegerlerSinirlanir() {
        assertThat(SayfalamaUtil.coz(-5, 0, Sort.unsorted()))
                .isEqualTo(PageRequest.of(0, SayfalamaUtil.VARSAYILAN_BOYUT));
        assertThat(SayfalamaUtil.coz(2, 100000, Sort.unsorted()).getPageSize())
                .isEqualTo(SayfalamaUtil.MAKS_BOYUT);
    }

    @Test
    void gecerliDegerlerVeSiralamaKorunur() {
        var pageable = SayfalamaUtil.coz(1, 25, Sort.by(Sort.Direction.DESC, "tarih"));
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("tarih").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
