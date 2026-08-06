package com.raspel.erp.config;

import com.raspel.erp.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class TenantChecker {

    public Long getCurrentSirketId() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return (Long) req.getAttribute("sirketId");
        } catch (Exception e) {
            return null;
        }
    }

    public void check(Long entitySirketId, String entityName) {
        Long currentSirketId = getCurrentSirketId();
        if (currentSirketId != null && entitySirketId != null && !currentSirketId.equals(entitySirketId)) {
            throw new ResourceNotFoundException(entityName + " bu sirkete ait degil");
        }
    }
}
