package com.raspel.erp.config;

import com.raspel.erp.config.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    /**
     * true ise bildirim/sohbet yayınları RabbitMQ STOMP broker üzerinden yapılır
     * (çok instance'ta senkron çalışır). Varsayılan: bellek içi simple broker.
     */
    @Value("${app.websocket.relay-enabled:false}")
    private boolean relayEnabled;

    @Value("${spring.rabbitmq.host:localhost}")
    private String relayHost;

    @Value("${spring.rabbitmq.username:guest}")
    private String relayUser;

    @Value("${spring.rabbitmq.password:guest}")
    private String relayPassword;

    @Value("${app.websocket.relay-port:61613}")
    private int relayPort;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (relayEnabled) {
            config.enableStompBrokerRelay("/topic")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayUser)
                    .setClientPasscode(relayPassword)
                    .setSystemLogin(relayUser)
                    .setSystemPasscode(relayPassword);
        } else {
            config.enableSimpleBroker("/topic");
        }
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new JwtHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor(jwtUtil));
    }
}
