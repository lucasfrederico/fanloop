package io.fanloop.backplane;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import io.fanloop.channel.ChannelRegistry;
import io.fanloop.metrics.FanloopMetrics;
import io.fanloop.ws.FanoutWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class BackplaneConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory cf) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        return container;
    }

    @Bean
    public RedisBackplane redisBackplane(RedisConnectionFactory cf,
                                         RedisMessageListenerContainer container,
                                         ChannelRegistry registry,
                                         FanoutWebSocketHandler handler,
                                         FanloopMetrics metrics,
                                         @Value("${fanloop.channel-prefix}") String prefix) {
        RedisBackplane backplane = new RedisBackplane(cf, container, registry, handler, metrics, prefix);
        backplane.start();
        return backplane;
    }
}
