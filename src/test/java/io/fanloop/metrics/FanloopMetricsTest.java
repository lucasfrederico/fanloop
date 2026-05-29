package io.fanloop.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FanloopMetricsTest {

    @Test
    void recordsPublishAndFanoutTimer() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        FanloopMetrics metrics = new FanloopMetrics(reg);
        metrics.incrementConnections();
        metrics.recordPublish();
        metrics.recordFanoutNanos(1_000_000L);
        assertThat(reg.get("fanloop.connections.active").gauge().value()).isEqualTo(1.0);
        assertThat(reg.get("fanloop.publishes").counter().count()).isEqualTo(1.0);
        assertThat(reg.get("fanloop.fanout.latency").timer().count()).isEqualTo(1L);
    }
}
