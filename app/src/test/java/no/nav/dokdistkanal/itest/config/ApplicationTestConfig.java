package no.nav.dokdistkanal.itest.config;

import no.nav.dokdistkanal.ApplicationConfig;
import no.nav.dokdistkanal.azure.AzureProperties;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties({
		AzureProperties.class,
		DokdistkanalProperties.class,
		MaskinportenProperties.class,
})
@Import({CacheTestConfig.class, RestTemplateTestConfig.class, ApplicationConfig.class})
@Profile("itest")
public class ApplicationTestConfig {
}
