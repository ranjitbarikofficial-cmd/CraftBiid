package com.craftbid.config;

import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketConfig(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    // 1. Authenticate STOMP CONNECT frame via JWT
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            authHeader = accessor.getPasscode();
                        }
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7).trim();
                            try {
                                Claims claims = Jwts.parser()
                                        .verifyWith(jwtService.getSigningKey())
                                        .build()
                                        .parseSignedClaims(token)
                                        .getPayload();

                                String email = claims.getSubject();
                                String role = claims.get("role", String.class);

                                if (email != null) {
                                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                            email,
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                    );
                                    accessor.setUser(auth);
                                }
                            } catch (Exception e) {
                                logger.debug("WebSocket STOMP authentication token invalid or expired: {}", e.getMessage());
                            }
                        }
                    }

                    // 2. Authorize STOMP SUBSCRIBE destinations (IDOR & Privacy Protection)
                    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        String destination = accessor.getDestination();
                        if (destination != null && destination.startsWith("/topic/users/")) {
                            String targetUserIdStr = destination.substring("/topic/users/".length());
                            Principal principal = accessor.getUser();
                            if (principal == null) {
                                throw new AccessDeniedException("Authentication required to subscribe to private user channels");
                            }

                            User user = userRepository.findByIdentifier(principal.getName())
                                    .orElseThrow(() -> new AccessDeniedException("User not found"));

                            if (user.getRole() != Role.ADMIN && !user.getId().toString().equals(targetUserIdStr)) {
                                logger.warn("Blocked unauthorized attempt by user {} to subscribe to {}", user.getId(), destination);
                                throw new AccessDeniedException("Access denied: You cannot subscribe to another user's private notification channel");
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
