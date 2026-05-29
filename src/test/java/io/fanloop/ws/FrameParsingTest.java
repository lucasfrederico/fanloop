package io.fanloop.ws;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FrameParsingTest {

    @Test
    void parsesSubscribeFrame() {
        FanoutWebSocketHandler.ControlFrame f =
            FanoutWebSocketHandler.parseFrame("{\"action\":\"subscribe\",\"channel\":\"orders\"}");
        assertThat(f.action()).isEqualTo("subscribe");
        assertThat(f.channel()).isEqualTo("orders");
    }

    @Test
    void parsesUnsubscribeFrame() {
        FanoutWebSocketHandler.ControlFrame f =
            FanoutWebSocketHandler.parseFrame("{\"action\":\"unsubscribe\",\"channel\":\"alerts\"}");
        assertThat(f.action()).isEqualTo("unsubscribe");
        assertThat(f.channel()).isEqualTo("alerts");
    }

    @Test
    void invalidJson_returnsNull() {
        assertThat(FanoutWebSocketHandler.parseFrame("not json")).isNull();
    }

    @Test
    void missingFields_returnsNull() {
        assertThat(FanoutWebSocketHandler.parseFrame("{\"action\":\"subscribe\"}")).isNull();
    }
}
