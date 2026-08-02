package com.raspel.erp.config;

import com.raspel.erp.config.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.Map;

@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionAttrs = (Map<String, Object>) accessor.getSessionAttributes();
            String token = sessionAttrs != null ? (String) sessionAttrs.get("token") : null;
            if (token != null && jwtUtil.validateToken(token)) {
                accessor.setUser(() -> jwtUtil.getUsernameFromToken(token));
            }
        }
        return message;
    }
}
