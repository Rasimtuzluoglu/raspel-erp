package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagServiceTest {

    private FeatureFlagService servis() {
        FeatureFlagService s = new FeatureFlagService();
        ReflectionTestUtils.setField(s, "enabledList", "");
        ReflectionTestUtils.setField(s, "disabledList", "");
        return s;
    }

    @Test
    void aktif_bilinenFlagVarsayilanAcik() {
        FeatureFlagService s = servis();
        assertTrue(s.aktif("churn-analizi"));
        assertTrue(s.aktif("api-token"));
    }

    @Test
    void aktif_bilinmeyenFlagKapali() {
        FeatureFlagService s = servis();
        assertFalse(s.aktif("bilinmeyen-ozellik"));
        assertFalse(s.aktif(null));
        assertFalse(s.aktif(""));
    }

    @Test
    void aktif_disabledListFlagKapatir() {
        FeatureFlagService s = servis();
        ReflectionTestUtils.setField(s, "disabledList", "churn-analizi");
        assertFalse(s.aktif("churn-analizi"));
    }

    @Test
    void aktif_enabledListBilinmeyeniAcar() {
        FeatureFlagService s = servis();
        ReflectionTestUtils.setField(s, "enabledList", "yeni-ozellik");
        assertTrue(s.aktif("yeni-ozellik"));
    }

    @Test
    void aktif_disabledEnableddenOnceGelir() {
        FeatureFlagService s = servis();
        ReflectionTestUtils.setField(s, "enabledList", "ozellik");
        ReflectionTestUtils.setField(s, "disabledList", "ozellik");
        assertFalse(s.aktif("ozellik"));
    }

    @Test
    void tumu_tumBilinenFlagleriDoner() {
        FeatureFlagService s = servis();
        Map<String, Boolean> tumu = s.tumu();
        assertTrue(tumu.containsKey("churn-analizi"));
        assertTrue(tumu.containsKey("api-token"));
        assertTrue(tumu.containsKey("butce-pdf"));
        assertTrue(tumu.containsKey("efatura-durum-sorgu"));
    }
}
