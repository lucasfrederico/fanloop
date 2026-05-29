package io.fanloop.backplane;

import io.fanloop.channel.ChannelRegistry;
import io.fanloop.metrics.FanloopMetrics;
import io.fanloop.ws.FanoutWebSocketHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class RedisBackplaneIT {

    static GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static LettuceConnectionFactory connectionFactory;

    @BeforeAll
    static void start() {
        redis.start();
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void stop() {
        connectionFactory.destroy();
        redis.stop();
    }

    @Test
    void publish_deliversToLocalSubscriber() throws Exception {
        ChannelRegistry registry = new ChannelRegistry();
        AtomicReference<String> delivered = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        FanoutWebSocketHandler handler = new FanoutWebSocketHandler(registry, new FanloopMetrics(new SimpleMeterRegistry())) {
            @Override
            public void deliver(String sessionId, String json) {
                delivered.set(json);
                latch.countDown();
            }
        };
        registry.subscribe("orders", "s1");

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.afterPropertiesSet();
        container.start();

        RedisBackplane backplane = new RedisBackplane(
            connectionFactory, container, registry, handler,
            new FanloopMetrics(new SimpleMeterRegistry()), "fanloop:");
        backplane.start();

        backplane.publish("orders", "{\"hello\":\"world\"}");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(delivered.get()).contains("hello");

        container.stop();
    }
}
