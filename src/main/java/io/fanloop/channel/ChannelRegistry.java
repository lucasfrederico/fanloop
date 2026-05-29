package io.fanloop.channel;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Per-replica map of channel name to the set of local session ids subscribed.
 * Thread-safe. Holds only the sessions connected to THIS replica.
 */
@Component
public class ChannelRegistry {

    private final ConcurrentHashMap<String, Set<String>> channels = new ConcurrentHashMap<>();

    public void subscribe(String channel, String sessionId) {
        channels.computeIfAbsent(channel, c -> new CopyOnWriteArraySet<>()).add(sessionId);
    }

    public void unsubscribe(String channel, String sessionId) {
        Set<String> sessions = channels.get(channel);
        if (sessions != null) {
            sessions.remove(sessionId);
            channels.compute(channel, (c, s) -> (s == null || s.isEmpty()) ? null : s);
        }
    }

    public void removeSession(String sessionId) {
        channels.forEach((channel, sessions) -> {
            sessions.remove(sessionId);
            channels.compute(channel, (c, s) -> (s == null || s.isEmpty()) ? null : s);
        });
    }

    public Set<String> sessionsFor(String channel) {
        return channels.getOrDefault(channel, Set.of());
    }

    public int channelCount() {
        return channels.size();
    }
}
