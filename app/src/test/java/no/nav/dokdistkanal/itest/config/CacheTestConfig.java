package no.nav.dokdistkanal.itest.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.boot.actuate.metrics.cache.CaffeineCacheMeterBinderProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.STS_CACHE;
import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer.HENT_PAALOGGINGSNIVAA;


/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */

@Profile("itest")
@Configuration
@EnableCaching
public class CacheTestConfig {

	@Autowired
	MeterRegistry registry;

	@Bean
	public CacheManager cacheManager() {
		CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(registry, Collections.singletonList(new CaffeineCacheMeterBinderProvider()));

		SimpleCacheManager cacheManager = new SimpleCacheManager();
		List<NoOpCache> noOpCaches = Arrays.asList(
				new NoOpCache(HENT_DOKKAT_INFO),
				new NoOpCache(STS_CACHE),
				new NoOpCache(HENT_PAALOGGINGSNIVAA));
		for (Cache cache : noOpCaches) {
			registrar.bindCacheToRegistry(cache);
		}

		cacheManager.setCaches(noOpCaches);

		return cacheManager;

	}
}
