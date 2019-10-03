package no.nav.dokdistkanal.itest.config;

import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer.HENT_PAALOGGINGSNIVAA;
import static no.nav.dokdistkanal.nais.NaisContract.STS_CACHE_NAME;

import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */

@Profile("itest")
@Configuration
@EnableCaching
public class CacheTestConfig {

	@Inject
	MeterRegistry registry;

	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory() {
		return new LettuceConnectionFactory();
	}

	@Bean
	public CacheManager cacheManager() {
		CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(registry, Collections.singletonList(new CaffeineCacheMeterBinderProvider()));

		SimpleCacheManager cacheManager = new SimpleCacheManager();
		List<NoOpCache> noOpCaches = Arrays.asList(
				new NoOpCache(HENT_DOKKAT_INFO),
				new NoOpCache(STS_CACHE_NAME),
				new NoOpCache(HENT_PAALOGGINGSNIVAA));
		for (Cache cache : noOpCaches) {
			registrar.bindCacheToRegistry(cache);
		}

		cacheManager.setCaches(noOpCaches);

		return cacheManager;

	}
}
