package com.craftbid.security;

import com.craftbid.config.WebSocketConfig;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WebSocket STOMP Security & Channel Authorization Tests")
public class WebSocketSecurityTest {

    private JwtService jwtService;
    private UserRepository userRepository;
    private WebSocketConfig webSocketConfig;
    private ChannelInterceptor interceptor;
    private MessageChannel dummyChannel;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "CraftBidSecretKeyForJwtAuthentication2026VerySecurePlatformKey");

        userRepository = mock(UserRepository.class);
        webSocketConfig = new WebSocketConfig(jwtService, userRepository);

        ChannelRegistration registration = new ChannelRegistration();
        webSocketConfig.configureClientInboundChannel(registration);

        java.util.List<?> list = (java.util.List<?>) ReflectionTestUtils.getField(registration, "interceptors");
        if (list != null && !list.isEmpty()) {
            interceptor = (ChannelInterceptor) list.get(0);
        }

        dummyChannel = mock(MessageChannel.class);
    }

    @Test
    @DisplayName("STOMP CONNECT with valid JWT should authenticate user on STOMP session")
    void shouldAuthenticateValidJwtOnStompConnect() {
        String token = jwtService.generateToken("customer@example.com", "CUSTOMER");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, dummyChannel);
        assertNotNull(result);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("customer@example.com", resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("STOMP CONNECT with invalid JWT should leave user unauthenticated without throwing")
    void shouldLeaveUnauthenticatedOnInvalidJwt() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid.fake.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, dummyChannel);
        assertNotNull(result);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    @DisplayName("Unauthenticated client should be ALLOWED to subscribe to public /topic/auctions")
    void shouldAllowUnauthenticatedPublicAuctionSubscription() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/auctions/5");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, dummyChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Unauthenticated client should be BLOCKED from private /topic/users/{id} channel")
    void shouldBlockUnauthenticatedPrivateUserSubscription() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/users/10");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, dummyChannel)
        );
        assertTrue(ex.getMessage().contains("Authentication required"));
    }

    @Test
    @DisplayName("Authenticated user should be BLOCKED from subscribing to ANOTHER user's private channel")
    void shouldBlockUserFromSubscribingToAnotherUsersPrivateChannel() {
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");
        user1.setRole(Role.CUSTOMER);

        when(userRepository.findByIdentifier("user1@example.com")).thenReturn(Optional.of(user1));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/users/2"); // attempting to spy on user 2
        accessor.setUser(new UsernamePasswordAuthenticationToken("user1@example.com", null));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, dummyChannel)
        );
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Authenticated user should be ALLOWED to subscribe to THEIR OWN private channel")
    void shouldAllowUserToSubscribeToTheirOwnPrivateChannel() {
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");
        user1.setRole(Role.CUSTOMER);

        when(userRepository.findByIdentifier("user1@example.com")).thenReturn(Optional.of(user1));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/users/1"); // subscribing to own channel
        accessor.setUser(new UsernamePasswordAuthenticationToken("user1@example.com", null));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, dummyChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("ADMIN should be ALLOWED to subscribe to any private user channel")
    void shouldAllowAdminToSubscribeToAnyPrivateChannel() {
        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@craftbid.co.in");
        admin.setRole(Role.ADMIN);

        when(userRepository.findByIdentifier("admin@craftbid.co.in")).thenReturn(Optional.of(admin));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/users/1");
        accessor.setUser(new UsernamePasswordAuthenticationToken("admin@craftbid.co.in", null));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, dummyChannel);
        assertNotNull(result);
    }
}
