package com.raspel.erp.config;

import com.raspel.erp.config.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SIRKET_TOPIC_PATTERN =
            Pattern.compile("^/topic/(bildirimler|sohbet)/(\\d+)$");

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionAttrs = (Map<String, Object>) accessor.getSessionAttributes();
            String token = sessionAttrs != null ? (String) sessionAttrs.get("token") : null;
            if (token != null && jwtUtil.validateToken(token)) {
                accessor.setUser(() -> jwtUtil.getUsernameFromToken(token));
                if (sessionAttrs != null) {
                    Long sirketId = jwtUtil.getSirketIdFromToken(token);
                    if (sirketId != null) {
                        sessionAttrs.put("sirketId", sirketId);
                    }
                }
            }
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return message;
            }
            Matcher m = SIRKET_TOPIC_PATTERN.matcher(destination);
            if (m.matches()) {
                Long aboneSirketId = Long.valueOf(m.group(2));
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionAttrs = (Map<String, Object>) accessor.getSessionAttributes();
                Long oturumSirketId = sessionAttrs != null ? (Long) sessionAttrs.get("sirketId") : null;
                if (oturumSirketId == null || !oturumSirketId.equals(aboneSirketId)) {
                    throw new MessageDeliveryException("Bu sirkete ait kanala abone olma yetkiniz yok");
                }
            }
        }
        return message;
    }
}
