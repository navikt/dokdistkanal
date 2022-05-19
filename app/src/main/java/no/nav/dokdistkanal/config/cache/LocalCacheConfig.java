package no.nav.dokdistkanal.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.Arrays;

import static java.util.concurrent.TimeUnit.MINUTES;
import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer.HENT_PAALOGGINGSNIVAA;
import static no.nav.dokdistkanal.nais.NaisContract.STS_CACHE_NAME;

@Configuration
@EnableCaching
@Profile({"local", "nais"})
public class LocalCacheConfig {
	public static final Duration DEFAULT_CACHE_EXPIRATION_TIME = Duration.ofMinutes(60);
	public static final Duration STS_CACHE_EXPIRATION_TIME = Duration.ofMinutes(50);
	public static final String AZURE_CLIENT_CREDENTIAL_TOKEN_CACHE = "AZUREAD";

	@Bean
	@Primary
	public CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(
				new CaffeineCache(HENT_PAALOGGINGSNIVAA, Caffeine.newBuilder()
						.expireAfterWrite(DEFAULT_CACHE_EXPIRATION_TIME)
						.maximumSize(1000)
						.build()),
				new CaffeineCache(HENT_DOKKAT_INFO, Caffeine.newBuilder()
						.expireAfterWrite(DEFAULT_CACHE_EXPIRATION_TIME)
						.build()),
				new CaffeineCache(STS_CACHE_NAME, Caffeine.newBuilder()
						.expireAfterWrite(STS_CACHE_EXPIRATION_TIME)
						.maximumSize(1)
						.build()),
				new CaffeineCache(AZURE_CLIENT_CREDENTIAL_TOKEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(50, MINUTES)
						.maximumSize(1)
						.build())
		));
		return cacheManager;

	}

}
