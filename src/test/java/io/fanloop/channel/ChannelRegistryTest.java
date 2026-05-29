package io.fanloop.channel;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChannelRegistryTest {

    @Test
    void subscribe_then_sessionsForChannel_returnsSession() {
        ChannelRegistry registry = new ChannelRegistry();
        registry.subscribe("orders", "session-1");
        assertThat(registry.sessionsFor("orders")).containsExactly("session-1");
    }

    @Test
    void unsubscribe_removesSession() {
        ChannelRegistry registry = new ChannelRegistry();
        registry.subscribe("orders", "session-1");
        registry.unsubscribe("orders", "session-1");
        assertThat(registry.sessionsFor("orders")).isEmpty();
    }

    @Test
    void removeSession_dropsFromAllChannels() {
        ChannelRegistry registry = new ChannelRegistry();
        registry.subscribe("orders", "session-1");
        registry.subscribe("alerts", "session-1");
        registry.removeSession("session-1");
        assertThat(registry.sessionsFor("orders")).isEmpty();
        assertThat(registry.sessionsFor("alerts")).isEmpty();
    }

    @Test
    void multipleSessions_onSameChannel() {
        ChannelRegistry registry = new ChannelRegistry();
        registry.subscribe("orders", "s1");
        registry.subscribe("orders", "s2");
        assertThat(registry.sessionsFor("orders")).containsExactlyInAnyOrder("s1", "s2");
    }
}
