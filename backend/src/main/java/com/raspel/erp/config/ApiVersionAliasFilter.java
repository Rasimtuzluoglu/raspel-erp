package com.raspel.erp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Faz 5: API sürümleme köprüsü. {@code /api/v1/...} isteklerini mevcut
 * {@code /api/...} denetleyicilerine yönlendirir. Böylece tüm controller'ları
 * değiştirmeden, sürümlenmiş istemciler çalışır ve mevcut istemciler bozulmaz.
 *
 * <p>Kimlik doğrulama/rate-limit filtreleri yolu {@code getRequestURI()} üzerinden
 * okuduğundan sarmalayıcı, isteği daha güvenlik zincirine girmeden normalleştirir.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ApiVersionAliasFilter extends OncePerRequestFilter {

    private static final String V1_ONEK = "/api/v1/";
    private static final String API_ONEK = "/api/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith(V1_ONEK)) {
            String hedef = API_ONEK + uri.substring(V1_ONEK.length());
            filterChain.doFilter(new V1AliasRequest(request, hedef), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** İstek yolunu {@code /api/v1/x -> /api/x} olarak normalize eden sarmalayıcı. */
    static final class V1AliasRequest extends HttpServletRequestWrapper {
        private final String hedefUri;

        V1AliasRequest(HttpServletRequest request, String hedefUri) {
            super(request);
            this.hedefUri = hedefUri;
        }

        @Override
        public String getRequestURI() {
            return hedefUri;
        }

        @Override
        public String getServletPath() {
            return hedefUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            String scheme = getScheme();
            int port = getServerPort();
            url.append(scheme).append("://").append(getServerName());
            if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                url.append(':').append(port);
            }
            url.append(hedefUri);
            return url;
        }
    }
}
