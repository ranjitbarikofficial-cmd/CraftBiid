package com.craftbid.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker with /topic (broadcast) and /queue (user-specific)
        config.enableSimpleBroker("/topic", "/queue");
        // Application destination prefix for incoming messages
        config.setApplicationDestinationPrefixes("/app");
        // User destination prefix for 1-to-1 messaging
        config.setUserDestinationPrefix("/user");
    }

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "https://craftbid.co.in",
            "https://*.craftbid.co.in",
            "https://*.vercel.app",
            "https://vercel.app",
            "https://*.onrender.com",
            "https://onrender.com"
    };

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native STOMP endpoint for direct WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOWED_ORIGINS);

        // STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(ALLOWED_ORIGINS)
                .withSockJS();
    }
}
