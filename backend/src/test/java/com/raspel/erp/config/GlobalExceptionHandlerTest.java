package com.raspel.erp.config;

import com.raspel.erp.repository.sistem.HataLogRepository;
import com.raspel.erp.service.sistem.HataBildirimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HataLogRepository hataLogRepository;
    private MockHttpServletRequest request;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        hataLogRepository = mock(HataLogRepository.class);
        ObjectProvider<HataLogRepository> repoProvider = mock(ObjectProvider.class);
        when(repoProvider.getIfAvailable()).thenReturn(hataLogRepository);

        HataBildirimService bildirimService = mock(HataBildirimService.class);
        ObjectProvider<HataBildirimService> bildirimProvider = mock(ObjectProvider.class);
        when(bildirimProvider.getIfAvailable()).thenReturn(bildirimService);

        handler = new GlobalExceptionHandler(repoProvider, bildirimProvider);
        request = new MockHttpServletRequest("GET", "/api/test");
    }

    @Test
    void handleRuntimeException_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("Bir hata oluştu");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Beklenmeyen bir sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin.", response.getBody().get("message"));
        assertEquals(500, response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
        verify(hataLogRepository).save(any());
    }

    @Test
    void handleDataIntegrityViolation_returnsConflictWithTurkishMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Veritabanı hatası. İşlem gerçekleştirilemedi.", response.getBody().get("message"));
        assertEquals(409, response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleValidation_returnsBadRequestWithFieldErrors() throws Exception {
        Method method = getClass().getDeclaredMethod("setUp");
        MethodParameter param = new MethodParameter(method, -1);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "username", "Kullanıcı adı zorunludur"));
        bindingResult.addError(new FieldError("testObject", "email", "Geçerli bir e-posta giriniz"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Doğrulama hatası", response.getBody().get("message"));
        assertEquals(400, response.getBody().get("status"));

        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertNotNull(errors);
        assertEquals("Kullanıcı adı zorunludur", errors.get("username"));
        assertEquals("Geçerli bir e-posta giriniz", errors.get("email"));
    }

    @Test
    void handleValidation_returnsBadRequestWhenNoFieldErrors() throws Exception {
        Method method = getClass().getDeclaredMethod("setUp");
        MethodParameter param = new MethodParameter(method, -1);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Doğrulama hatası", response.getBody().get("message"));
        assertTrue(((Map<String, String>) response.getBody().get("errors")).isEmpty());
    }
}
