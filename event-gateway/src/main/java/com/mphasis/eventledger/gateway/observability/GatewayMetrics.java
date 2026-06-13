package com.mphasis.eventledger.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GatewayMetrics {

    private final Counter acceptedEvents;
    private final Counter duplicateEvents;
    private final Counter downstreamFailures;

    public GatewayMetrics(MeterRegistry registry) {
        this.acceptedEvents = Counter.builder("event_gateway_events_accepted_total")
                .description("Number of new events accepted by the gateway")
                .register(registry);
        this.duplicateEvents = Counter.builder("event_gateway_events_duplicate_total")
                .description("Number of duplicate event submissions served idempotently")
                .register(registry);
        this.downstreamFailures = Counter.builder("event_gateway_account_service_failures_total")
                .description("Number of Account Service failures observed by the gateway")
                .register(registry);
    }

    public void eventAccepted() {
        acceptedEvents.increment();
    }

    public void duplicateEvent() {
        duplicateEvents.increment();
    }

    public void downstreamFailure() {
        downstreamFailures.increment();
    }
}
