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
            // Tercih 1: ?token= parametresi (eski istemciler)
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token == null || token.isBlank()) {
                // Tercih 2: httpOnly jwt cookie (aynı-orijin SockJS bağlantıları)
                Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if ("jwt".equals(c.getName())) {
                            token = c.getValue();
                            break;
                        }
                    }
                }
            }
            if (token != null && !token.isBlank()) {
                attributes.put("token", token);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
