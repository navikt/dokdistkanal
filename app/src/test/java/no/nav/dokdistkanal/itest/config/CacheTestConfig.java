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

import static java.util.concurrent.TimeUnit.MINUTES;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.HENT_DOKUMENTTYPE_INFO_CACHE;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer.HENT_PAALOGGINGSNIVAA;

@Configuration
@EnableCaching
@Profile({"itest"})
public class CacheTestConfig {

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(HENT_DOKUMENTTYPE_INFO_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(HENT_PAALOGGINGSNIVAA, Caffeine.newBuilder()
						.expireAfterWrite(0, MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(MASKINPORTEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, MINUTES)
						.maximumSize(0)
						.build())
				));
		return manager;
	}
}
