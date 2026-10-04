package com.raspel.erp.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Neden içeren kurucu. Teknik detayı log'a yazarken kullanıcıya anlamlı
     * mesaj döndürmek için gerekir (ör. otomatik muhasebe fişi oluşturulurken
     * DB kısıt ihlali). `GlobalExceptionHandler` yalnızca mesajı kullanıcıya
     * döner; stack trace log'a yazılır.
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
