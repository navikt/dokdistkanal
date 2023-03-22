package no.nav.dokdistkanal.consumer.pdl;


import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.sts.StsRestConsumer;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.PdlHentPersonTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";
	private static final String HEADER_PDL_TEMA = "Tema";

	private final RestTemplate restTemplate;
	private final StsRestConsumer stsConsumer;
	private final String pdlUrl;

	@Autowired
	public PdlGraphQLConsumer(RestTemplateBuilder restTemplateBuilder, StsRestConsumer stsConsumer, @Value("${pdl.url}") String pdlUrl) {
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(2L))
				.setReadTimeout(Duration.ofSeconds(5L))
				.build();
		this.stsConsumer = stsConsumer;
		this.pdlUrl = pdlUrl;
	}

	@Retryable(retryFor = HttpServerErrorException.class)
	public HentPersoninfo hentPerson(final String aktoerId, final String tema) {
		try {
			final UriComponents uri = UriComponentsBuilder.fromHttpUrl(pdlUrl).build();
			final String serviceUserToken = "Bearer " + stsConsumer.getOidcToken();
			final RequestEntity<PDLRequest> requestEntity = RequestEntity.post(uri.toUri())
					.accept(APPLICATION_JSON)
					.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
					.header(AUTHORIZATION, serviceUserToken)
					.header(NAV_CONSUMER_TOKEN, serviceUserToken)
					.header(HEADER_PDL_TEMA, tema)
					.body(mapRequest(aktoerId));

			log.debug("Henter personinfo for aktørId={}", aktoerId);

			final PDLHentPersonResponse response = requireNonNull(restTemplate.exchange(requestEntity, PDLHentPersonResponse.class).getBody());

			if (Objects.isNull(response.getErrors()) || response.getErrors().isEmpty()) {
				return mapPersonInfo(response);
			} else {
				return null;
			}
		} catch (HttpClientErrorException e) {
			throw new PdlFunctionalException("Kunne ikke hente person fra pdl.", e);
		} catch (HttpServerErrorException e) {
			throw new PdlHentPersonTechnicalException("Teknisk feil ved kall mot PDL.", e);
		}

	}

	private HentPersoninfo mapPersonInfo(PDLHentPersonResponse response) {
		if (Objects.isNull(response.getData().getHentPerson())) {
			throw new PdlFunctionalException("Kunne ikke hente person fra Pdl" + response.getErrors());
		} else {
			PDLHentPersonResponse.HentPerson hentPerson = response.getData().getHentPerson();
			return HentPersoninfo.builder()
					.doedsdato(hentPerson.getDoedsfall() == null ? null : hentPerson.getDoedsfall().stream()
							.map(PDLHentPersonResponse.Doedsfall::getDoedsdato)
							.filter(Objects::nonNull)
							.findAny().orElse(null))
					.foedselsdato(hentPerson.getFoedsel() == null ? null : hentPerson.getFoedsel().stream()
							.map(PDLHentPersonResponse.Foedsel::getFoedselsdato)
							.filter(Objects::nonNull)
							.findAny().orElse(null))
					.build();
		}
	}

	private PDLRequest mapRequest(final String aktoerId) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", aktoerId);

		return PDLRequest.builder().query("""
				query hentPerson($ident: ID!) {
				  hentPerson(ident: $ident) {
				     doedsfall {
				        doedsdato
				     }
				     foedsel {
				        foedselsdato
				     }
				  }
				}""").variables(variables).build();
	}
}
