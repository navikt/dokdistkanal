package no.nav.dokdistkanal;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import io.prometheus.client.spring.web.EnablePrometheusTiming;
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
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaRestComsumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
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
		SikkerhetsnivaaRestComsumer.class,
		DokumentTypeInfoConsumer.class,
		RestConsumerConfig.class
})
@EnablePrometheusEndpoint
@EnablePrometheusTiming
@EnableSpringBootMetricsCollector
@Configuration
public class ApplicationConfig {
}
