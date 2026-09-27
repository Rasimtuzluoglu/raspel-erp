package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock private RestTemplate restTemplate;

    @Test
    void gonder_saglayiciYoksaHataFirlatir() {
        SmsService servis = new SmsService(restTemplate);
        ReflectionTestUtils.setField(servis, "smsUrl", "");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> servis.gonder("5551234567", "Test"));

        assertTrue(ex.getMessage().contains("tanımlı değil"));
        verify(restTemplate, never()).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    void gonder_saglayiciVarsaHttpIleGonderir() {
        SmsService servis = new SmsService(restTemplate);
        ReflectionTestUtils.setField(servis, "smsUrl", "https://sms.example.com/gonder");
        ReflectionTestUtils.setField(servis, "smsApiKey", "key");
        ReflectionTestUtils.setField(servis, "smsGonderen", "RasPel");

        servis.gonder("5551234567", "Test mesajı");

        verify(restTemplate).postForEntity(eq("https://sms.example.com/gonder"), any(), eq(String.class));
    }

    @Test
    void gonder_bosTelefonReddedilir() {
        SmsService servis = new SmsService(restTemplate);
        assertThrows(BusinessException.class, () -> servis.gonder(" ", "Test"));
    }
}
