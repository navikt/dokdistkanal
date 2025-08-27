package no.nav.dokdistkanal.consumer.serviceregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.altinn.maskinporten.MaskinportenConsumer;
import no.nav.dokdistkanal.exceptions.technical.ServiceRegistryTechnicalException;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;

@Slf4j
@Component
public class ServiceRegistryConsumer {

	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente mottakerInfo fra service registry. Teknisk feil: ";
	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente mottakerInfo fra service registry. Funksjonell feil: ";

	public static final String DPO_SCOPE = "move/dpo.read";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final MaskinportenConsumer maskinportenConsumer;

	public ServiceRegistryConsumer(RestClient.Builder restClientBuilder,
								   DokdistkanalProperties dokdistkanalProperties,
								   MaskinportenConsumer maskinportenConsumer,
								   ClientHttpRequestFactory clientHttpRequestFactory,
								   ObjectMapper objectMapper) {
		this.maskinportenConsumer = maskinportenConsumer;
		this.restClient = restClientBuilder
				.baseUrl(dokdistkanalProperties.getServiceRegistry().getUrl())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setContentType(MediaType.APPLICATION_JSON);
				})
				.requestFactory(clientHttpRequestFactory)
				.build();
		this.objectMapper = objectMapper;
	}

	@Retryable(retryFor = ServiceRegistryTechnicalException.class)
	public IdentifierResource getIdentifierResource(final String orgnummer, final String processIdentifier) {
		return restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/identifier/{orgnummer}/process/" + processIdentifier)
						.build(orgnummer))
				.headers(httpHeaders -> {
					httpHeaders.set(NAV_CALLID, MDC.get(CALL_ID));
					httpHeaders.setBearerAuth(maskinportenConsumer.getMaskinportenToken(DPO_SCOPE));
				})
				.exchange((req, res) -> {
					if (res.getStatusCode().isError()) {
						ProblemDetail problemDetail = objectMapper.readValue(res.getBody(), ProblemDetail.class);
						if (res.getStatusCode().is5xxServerError()) {
							log.error(TEKNISK_FEIL_ERROR_MESSAGE + "{}", problemDetail.getDetail());
							throw new ServiceRegistryTechnicalException(TEKNISK_FEIL_ERROR_MESSAGE + problemDetail);
						}
						log.error(FUNKSJONELL_FEIL_ERROR_MESSAGE + "{}", problemDetail.getDetail());
						return null;
					}
					return res.bodyTo(IdentifierResource.class);
				});
	}
}
