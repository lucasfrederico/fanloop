package io.fanloop.backplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fanloop.channel.ChannelRegistry;
import io.fanloop.metrics.FanloopMetrics;
import io.fanloop.ws.FanoutWebSocketHandler;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes events to Redis and forwards inbound Redis messages to local subscribers.
 * Every replica runs one of these and subscribes to the same channel pattern, so a
 * publish on any replica fans out to subscribers on all replicas.
 */
@Component
public class RedisBackplane {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringRedisTemplate template;
    private final RedisMessageListenerContainer listenerContainer;
    private final ChannelRegistry registry;
    private final FanoutWebSocketHandler handler;
    private final FanloopMetrics metrics;
    private final String prefix;

    public RedisBackplane(RedisConnectionFactory connectionFactory,
                          RedisMessageListenerContainer listenerContainer,
                          ChannelRegistry registry,
                          FanoutWebSocketHandler handler,
                          FanloopMetrics metrics,
                          String prefix) {
        this.template = new StringRedisTemplate(connectionFactory);
        this.listenerContainer = listenerContainer;
        this.registry = registry;
        this.handler = handler;
        this.metrics = metrics;
        this.prefix = prefix;
    }

    public void start() {
        MessageListenerAdapter adapter = new MessageListenerAdapter((org.springframework.data.redis.connection.MessageListener)
            (message, pattern) -> {
                String redisChannel = new String(message.getChannel());
                String body = new String(message.getBody());
                onMessage(redisChannel, body);
            });
        listenerContainer.addMessageListener(adapter, new PatternTopic(prefix + "*"));
    }

    /** Publish an event to a channel. Body is the raw event payload (JSON). */
    public void publish(String channel, String payload) {
        metrics.recordPublish();
        template.convertAndSend(prefix + channel, payload);
    }

    /** Forward a Redis message to local subscribers of the channel. */
    void onMessage(String redisChannel, String payload) {
        long start = System.nanoTime();
        String channel = redisChannel.substring(prefix.length());
        String envelope = envelope(channel, payload);
        for (String sessionId : registry.sessionsFor(channel)) {
            handler.deliver(sessionId, envelope);
        }
        metrics.recordFanoutNanos(System.nanoTime() - start);
    }

    private String envelope(String channel, String payload) {
        try {
            Map<String, Object> map = Map.of("channel", channel, "data", MAPPER.readTree(payload));
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            try {
                return MAPPER.writeValueAsString(Map.of("channel", channel, "data", payload));
            } catch (Exception ignored) {
                return "{\"channel\":\"" + channel + "\"}";
            }
        }
    }
}
