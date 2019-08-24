package no.nav.dokdistkanal.metrics;

import org.springframework.stereotype.Component;

@Component
public class CacheMissMarker {
	@CacheMiss
	public void cacheMiss(String cacheName) {
	}

}
