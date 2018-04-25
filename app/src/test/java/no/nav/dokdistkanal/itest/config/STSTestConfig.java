package no.nav.dokdistkanal.itest.config;

import no.nav.dokdistkanal.config.sts.STSConfig;
import org.apache.cxf.ws.security.trust.STSClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import wiremock.org.eclipse.jetty.server.session.JDBCSessionIdManager;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Component
@Profile("itest")
public class STSTestConfig extends STSConfig {
	@Override
	public void configureSTS(Object port){
	}
}