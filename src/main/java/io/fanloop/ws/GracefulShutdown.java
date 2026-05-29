package io.fanloop.ws;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class GracefulShutdown {

    private final FanoutWebSocketHandler handler;

    public GracefulShutdown(FanoutWebSocketHandler handler) {
        this.handler = handler;
    }

    @PreDestroy
    public void onShutdown() {
        handler.closeAll();
    }
}
