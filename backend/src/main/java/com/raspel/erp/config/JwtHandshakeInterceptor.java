package com.raspel.erp.config;

import jakarta.servlet.http.Cookie;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // Token yalnızca httpOnly jwt cookie'den okunur. Query-string token'ı
            // (eski ?token= parametresi) log/proxy sızıntısı riski nedeniyle kaldırıldı.
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("jwt".equals(c.getName())) {
                        if (c.getValue() != null && !c.getValue().isBlank()) {
                            attributes.put("token", c.getValue());
                        }
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
