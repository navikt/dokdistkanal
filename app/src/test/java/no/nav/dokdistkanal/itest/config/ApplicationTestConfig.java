package no.nav.dokdistkanal.itest.config;

import org.apache.cxf.BusFactory;
import org.apache.cxf.ws.security.trust.STSClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Configuration
@Import({CacheTestConfig.class, RestTemplateTestConfig.class})
@Profile("itest")
public class ApplicationTestConfig {
	@Bean
	public STSClient stsClient() {
		return new STSClient(BusFactory.newInstance().createBus());
	}

}
