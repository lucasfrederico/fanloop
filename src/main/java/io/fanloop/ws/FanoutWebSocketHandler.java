package io.fanloop.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fanloop.channel.ChannelRegistry;
import io.fanloop.metrics.FanloopMetrics;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FanoutWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChannelRegistry registry;
    private final FanloopMetrics metrics;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public FanoutWebSocketHandler(ChannelRegistry registry, FanloopMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics;
    }

    public record ControlFrame(String action, String channel) {}

    /** Parses a control frame; returns null on invalid JSON or missing fields. */
    public static ControlFrame parseFrame(String payload) {
        try {
            JsonNode node = MAPPER.readTree(payload);
            JsonNode action = node.get("action");
            JsonNode channel = node.get("channel");
            if (action == null || channel == null) {
                return null;
            }
            return new ControlFrame(action.asText(), channel.asText());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        metrics.incrementConnections();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ControlFrame frame = parseFrame(message.getPayload());
        if (frame == null) {
            return;
        }
        switch (frame.action()) {
            case "subscribe" -> registry.subscribe(frame.channel(), session.getId());
            case "unsubscribe" -> registry.unsubscribe(frame.channel(), session.getId());
            default -> { /* ignore unknown actions */ }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        registry.removeSession(session.getId());
        metrics.decrementConnections();
    }

    /** Called by the backplane to deliver an event to a local subscriber. */
    public void deliver(String sessionId, String json) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                // best-effort fan-out; drop on write failure
            }
        }
    }

    /** Used during graceful shutdown to close all live connections. */
    public void closeAll() {
        sessions.values().forEach(s -> {
            try {
                if (s.isOpen()) {
                    s.close(CloseStatus.GOING_AWAY);
                }
            } catch (IOException ignored) { }
        });
    }
}
