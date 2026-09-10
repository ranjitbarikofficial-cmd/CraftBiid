package com.craftbid.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuctionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public AuctionEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast an event to all subscribers of a specific auction room
     */
    public void publishAuctionEvent(Long auctionId, String eventType, Object data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("auctionId", auctionId);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);

        // Cast to Object to disambiguate from (Object, Map<String, Object> headers) overload
        messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, (Object) payload);
        messagingTemplate.convertAndSend("/topic/auctions", (Object) payload);
    }

    /**
     * Send a private notification event to a specific user
     */
    public void publishUserEvent(Long userId, String eventType, Object data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("userId", userId);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);

        messagingTemplate.convertAndSend("/topic/users/" + userId, (Object) payload);
    }
}
