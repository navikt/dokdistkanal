package no.nav.dokdistkanal.config.cxf;

import no.nav.dokdistkanal.config.fasit.DigitalKontaktinfoV1Alias;
import no.nav.dokdistkanal.config.fasit.NavAppCertAlias;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

@Configuration
public class DigitalKontaktinformasjonEndpointConfig extends AbstractCxfEndpointConfig {

	public static final String WSDL_URL = "wsdl/no/nav/tjeneste/virksomhet/digitalKontaktinformasjon/v1/Binding.wsdl";

	public static final String BINDING_NAMESPACE_URI = "http://nav.no/tjeneste/virksomhet/digitalKontaktinformasjon/v1/Binding";
	public static final QName SERVICE = new QName(BINDING_NAMESPACE_URI, "DigitalKontaktinformasjon_v1");
	public static final QName PORT = new QName(BINDING_NAMESPACE_URI, "DigitalKontaktinformasjon_v1Port");

	@Bean
	public DigitalKontaktinformasjonV1 hentDigitalKontaktinformasjon(DigitalKontaktinfoV1Alias digitalKontaktinfoV1Alias, NavAppCertAlias navAppCertAlias) {

		navAppCertAlias.postConstruct();
		setWsdlUrl(WSDL_URL);
		setEndpointName(PORT);
		setServiceName(SERVICE);
		setAdress(digitalKontaktinfoV1Alias.getEndpointurl());
		setReceiveTimeout(digitalKontaktinfoV1Alias.getReadtimeoutms());
		setConnectTimeout(digitalKontaktinfoV1Alias.getConnecttimeoutms());
		addFeature(new WSAddressingFeature());

		DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1 = createPort(DigitalKontaktinformasjonV1.class);
		configureSTSSamlToken(digitalKontaktinformasjonV1);

		return digitalKontaktinformasjonV1;
	}
}
