package no.nav.dokdistkanal.itest.config;

import static no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer.HENT_SIKKER_DIGITAL_POSTADRESSE;
import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer.HENT_PERSON;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaRestComsumer.HENT_PAALOGGINGSNIVAA;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Arrays;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */

@Profile("itest")
@Configuration
@EnableCaching
public class CacheTestConfig {


	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory() {
		return new LettuceConnectionFactory();
	}

	@Bean
	public CacheManager cacheManager() {

		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(
				new NoOpCache(HENT_SIKKER_DIGITAL_POSTADRESSE),
				new NoOpCache(HENT_PERSON),
				new NoOpCache(HENT_DOKKAT_INFO),
				new NoOpCache(HENT_PAALOGGINGSNIVAA)));
		return cacheManager;

	}
}
