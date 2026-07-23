package com.raspel.erp.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entityAdi, Long id) {
        super(entityAdi + " bulunamadı: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
