package no.nav.dokdistkanal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import no.nav.dokdistkanal.config.RestConsumerConfig;
import no.nav.dokdistkanal.config.cxf.DigitalKontaktinformasjonEndpointConfig;
import no.nav.dokdistkanal.config.cxf.PersonV3EndpointConfig;
import no.nav.dokdistkanal.config.fasit.DigitalKontaktinfoV1Alias;
import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV4Alias;
import no.nav.dokdistkanal.config.fasit.NavAppCertAlias;
import no.nav.dokdistkanal.config.fasit.PersonV3Alias;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.config.fasit.SikkerhetsnivaaV1Alias;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.metrics.DokTimedAspect;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties({
		PersonV3Alias.class,
		DokumenttypeInfoV4Alias.class,
		DigitalKontaktinfoV1Alias.class,
		NavAppCertAlias.class,
		ServiceuserAlias.class,
		SikkerhetsnivaaV1Alias.class
})
@Import({
		PersonV3Consumer.class,
		PersonV3EndpointConfig.class,
		DigitalKontaktinformasjonConsumer.class,
		DigitalKontaktinformasjonEndpointConfig.class,
		SikkerhetsnivaaConsumer.class,
		DokumentTypeInfoConsumer.class,
		RestConsumerConfig.class
})
@Configuration
@EnableAspectJAutoProxy
@EnableAutoConfiguration
public class ApplicationConfig {

	@Bean
	public DokTimedAspect timedAspect(MeterRegistry meterRegistry) {
		return new DokTimedAspect(meterRegistry);
	}

	@Bean
	JvmThreadMetrics threadMetrics(){
		return new JvmThreadMetrics();
	}
}
