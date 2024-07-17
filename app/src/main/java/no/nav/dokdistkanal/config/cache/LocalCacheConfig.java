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

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Configuration
@EnableCaching
@Profile({"local", "nais"})
public class LocalCacheConfig {
	public static final String MASKINPORTEN_CACHE = "maskinportenCache";
	public static final String DOKMET_CACHE = "dokmetCache";

	@Bean
	@Primary
	public CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(
				new CaffeineCache(DOKMET_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(24, HOURS)
						.build()),
				new CaffeineCache(MASKINPORTEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(50, MINUTES)
						.build())
		));
		return cacheManager;

	}

}
