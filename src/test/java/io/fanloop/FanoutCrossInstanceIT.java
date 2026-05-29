package io.fanloop;

import io.fanloop.backplane.RedisBackplane;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FanoutCrossInstanceIT {

    static GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    static LettuceConnectionFactory cf;

    @BeforeAll static void start() {
        redis.start();
        cf = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        cf.afterPropertiesSet();
    }

    @AfterAll static void stop() { cf.destroy(); redis.stop(); }

    private RedisBackplane buildInstance(ChannelRegistry registry, FanoutWebSocketHandler handler) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        container.afterPropertiesSet();
        container.start();
        RedisBackplane bp = new RedisBackplane(cf, container, registry, handler,
            new FanloopMetrics(new SimpleMeterRegistry()), "fanloop:");
        bp.start();
        return bp;
    }

    @Test
    void publishOnInstanceA_reachesSubscriberOnInstanceB() throws Exception {
        ChannelRegistry regA = new ChannelRegistry();
        FanoutWebSocketHandler handlerA = new FanoutWebSocketHandler(regA, new FanloopMetrics(new SimpleMeterRegistry()));
        RedisBackplane instanceA = buildInstance(regA, handlerA);

        ChannelRegistry regB = new ChannelRegistry();
        AtomicReference<String> got = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        FanoutWebSocketHandler handlerB = new FanoutWebSocketHandler(regB, new FanloopMetrics(new SimpleMeterRegistry())) {
            @Override public void deliver(String sessionId, String json) { got.set(json); latch.countDown(); }
        };
        buildInstance(regB, handlerB);
        regB.subscribe("orders", "sessionOnB");

        Thread.sleep(300);

        instanceA.publish("orders", "{\"id\":42}");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(got.get()).contains("\"channel\":\"orders\"").contains("42");
    }
}
