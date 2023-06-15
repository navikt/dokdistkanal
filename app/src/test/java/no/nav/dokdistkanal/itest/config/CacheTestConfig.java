package no.nav.dokdistkanal.itest.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.STS_CACHE;
import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer.HENT_PAALOGGINGSNIVAA;

@Configuration
@EnableCaching
@Profile({"itest"})
public class CacheTestConfig {

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(HENT_DOKKAT_INFO, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(STS_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(HENT_PAALOGGINGSNIVAA, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(MASKINPORTEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build())
		));
		return manager;
	}
}
