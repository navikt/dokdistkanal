package no.nav.dokdistkanal.itest.config;

import no.nav.dokdistkanal.ApplicationConfig;
import no.nav.dokdistkanal.azure.AzureProperties;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
@Import({ApplicationConfig.class})
@Profile("itest")
public class ApplicationTestConfig {
	private static final Instant FIXED_INSTANT = Instant.parse("2023-08-15T12:00:00Z");

	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(FIXED_INSTANT, ZoneId.of("Europe/Oslo"));
	}
}
