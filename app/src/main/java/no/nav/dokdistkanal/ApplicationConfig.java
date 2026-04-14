package no.nav.dokdistkanal;

import no.nav.dokdistkanal.certificate.KeyStoreProperties;
import no.nav.dokdistkanal.config.RestWebMvcConfig;
import no.nav.dokdistkanal.config.nais.NaisProperties;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties({
		DokdistkanalProperties.class,
		NaisProperties.class,
		MaskinportenProperties.class,
		KeyStoreProperties.class
})
@Import(RestWebMvcConfig.class)
@Configuration
public class ApplicationConfig {
}
