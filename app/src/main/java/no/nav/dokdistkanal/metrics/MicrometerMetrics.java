package no.nav.dokdistkanal.metrics;

import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Slf4j
@Component
public class MicrometerMetrics {
	@Inject
	private MeterRegistry registry;

	public void cacheMiss(String cacheName) {
		Counter.builder("dok_request_total_counter")
				.tag("process", cacheName)
				.tag("type", "cacheCounter")
				.tag("consumer_name", getConsumerId())
				.tag("event", "cacheMiss")
				.register(registry).increment();
	}
}
