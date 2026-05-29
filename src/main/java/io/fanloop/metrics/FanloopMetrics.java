package io.fanloop.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FanloopMetrics {

    private final AtomicInteger connections = new AtomicInteger();
    private final Counter publishes;
    private final Timer fanoutLatency;

    public FanloopMetrics(MeterRegistry registry) {
        registry.gauge("fanloop.connections.active", connections);
        this.publishes = Counter.builder("fanloop.publishes").register(registry);
        this.fanoutLatency = Timer.builder("fanloop.fanout.latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incrementConnections() { connections.incrementAndGet(); }
    public void decrementConnections() { connections.decrementAndGet(); }
    public void recordPublish() { publishes.increment(); }
    public void recordFanoutNanos(long nanos) { fanoutLatency.record(nanos, TimeUnit.NANOSECONDS); }
}
