package no.nav.dokkanalvalg;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import io.prometheus.client.spring.web.EnablePrometheusTiming;
import no.nav.dokkanalvalg.config.cxf.DigitalKontaktinformasjonEndpointConfig;
import no.nav.dokkanalvalg.config.cxf.PersonV3EndpointConfig;
import no.nav.dokkanalvalg.config.fasit.DigitalKontaktinfoV1Alias;
import no.nav.dokkanalvalg.config.fasit.DokumenttypeInfoV3Alias;
import no.nav.dokkanalvalg.config.fasit.NavAppCertAlias;
import no.nav.dokkanalvalg.config.fasit.PersonV3Alias;
import no.nav.dokkanalvalg.config.fasit.ServiceuserAlias;
import no.nav.dokkanalvalg.consumer.personv3.PersonV3Consumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties({
		PersonV3Alias.class,
		DokumenttypeInfoV3Alias.class,
		DigitalKontaktinfoV1Alias.class,
		NavAppCertAlias.class,
		ServiceuserAlias.class
})
@Import({
		PersonV3Consumer.class,
		PersonV3EndpointConfig.class,
		DigitalKontaktinformasjonEndpointConfig.class
})
@EnablePrometheusEndpoint
@EnablePrometheusTiming
@EnableSpringBootMetricsCollector
@Configuration
public class ApplicationConfig {
}
