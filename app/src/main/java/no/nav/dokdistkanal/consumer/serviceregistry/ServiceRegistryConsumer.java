package no.nav.dokdistkanal.consumer.serviceregistry;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenConsumer;
import no.nav.dokdistkanal.exceptions.technical.ServiceRegistryTechnicalException;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;
import static no.nav.dokdistkanal.consumer.serviceregistry.IdentifierResource.ServiceIdentifier.DPO;

@Slf4j
@Component
public class ServiceRegistryConsumer {

	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente mottakerInfo fra service registry. Teknisk feil: ";

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final MaskinportenConsumer maskinportenConsumer;

	public ServiceRegistryConsumer(RestClient.Builder restClientBuilder,
								   DokdistkanalProperties dokdistkanalProperties,
								   MaskinportenConsumer maskinportenConsumer,
								   JdkClientHttpRequestFactory jdkClientHttpRequestFactory,
								   JsonMapper jsonMapper) {
		this.maskinportenConsumer = maskinportenConsumer;
		this.restClient = restClientBuilder
				.baseUrl(dokdistkanalProperties.getServiceRegistry().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
				.requestFactory(jdkClientHttpRequestFactory)
				.build();
		this.jsonMapper = jsonMapper;
	}

	@Retryable(includes = ServiceRegistryTechnicalException.class)
	public IdentifierResource getIdentifierResource(final String orgnummer, final String processIdentifier) {
		return restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/identifier/{orgnummer}/process/" + processIdentifier)
						.build(orgnummer))
				.headers(httpHeaders -> {
					httpHeaders.set(NAV_CALLID, MDC.get(CALL_ID));
					httpHeaders.setBearerAuth(maskinportenConsumer.getMaskinportenToken(DPO));
				})
				.exchange((req, res) -> {
					if (res.getStatusCode().isError()) {
						ProblemDetail problemDetail = jsonMapper.readValue(res.getBody(), ProblemDetail.class);
						if (res.getStatusCode().is5xxServerError()) {
							log.error(TEKNISK_FEIL_ERROR_MESSAGE + " status={} og feilmelding={}", res.getStatusCode(), problemDetail.getDetail());
							throw new ServiceRegistryTechnicalException(TEKNISK_FEIL_ERROR_MESSAGE + problemDetail);
						}
						return null;
					}
					return res.bodyTo(IdentifierResource.class);
				});
	}
}
