package no.nav.dokdistkanal.config.cache;

import static no.nav.dokdistkanal.config.cache.CacheConfig.DEFAULT_CACHE_EXPIRATION_TIME;
import static no.nav.dokdistkanal.config.cache.CacheConfig.HENT_PERSON_CACHE_EXPIRATION_TIME;
import static no.nav.dokdistkanal.config.cache.CacheConfig.STS_CACHE_EXPIRATION_TIME;
import static no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer.HENT_SIKKER_DIGITAL_POSTADRESSE;
import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer.HENT_PERSON;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer.HENT_PAALOGGINGSNIVAA;
import static no.nav.dokdistkanal.nais.NaisContract.STS_CACHE_NAME;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.boot.actuate.metrics.cache.CaffeineCacheMeterBinderProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cachemanager for bruk ved lokalt kjøring av applikasjonen.
 *
 * Redis cache krever en Redis server som for å fungere. Redis serveren som kjører på nais er ikke eksponert ut og er derfor ikke mulig å aksessere lokalt.
 * For å slippe å starte opp Redis server lokalt så vil denne klassen configurere cachemanager som kan kjøre ved lokalt kjøring av applikasjonen.
 *
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Profile("local")
@Configuration
@EnableCaching
public class LocalCacheConfig {

	@Inject
	MeterRegistry registry;

	@Bean
	@Primary
	public CacheManager cacheManager() {
		CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(registry, Collections.singletonList(new CaffeineCacheMeterBinderProvider()));
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		List<CaffeineCache> caffeineCaches = Arrays.asList(
				new CaffeineCache(HENT_SIKKER_DIGITAL_POSTADRESSE, Caffeine.newBuilder()
						.expireAfterWrite(DEFAULT_CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
						.build()),
				new CaffeineCache(HENT_PAALOGGINGSNIVAA, Caffeine.newBuilder()
						.expireAfterWrite(DEFAULT_CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
						.build()),
				new CaffeineCache(HENT_PERSON, Caffeine.newBuilder()
						.expireAfterWrite(HENT_PERSON_CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
						.build()),
				new CaffeineCache(HENT_PERSON, Caffeine.newBuilder()
						.expireAfterWrite(DEFAULT_CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
						.build()),
				new CaffeineCache(HENT_DOKKAT_INFO, Caffeine.newBuilder()
						.expireAfterWrite(DEFAULT_CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
						.build()));
		CaffeineCache caffeineCache = new CaffeineCache(STS_CACHE_NAME, Caffeine.newBuilder()
				.expireAfterWrite(STS_CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
				.build());
		registrar.bindCacheToRegistry(caffeineCache);

		for (Cache cache : caffeineCaches) {
			registrar.bindCacheToRegistry(cache);
		}
		cacheManager.setCaches(caffeineCaches);

		return cacheManager;

	}

}
