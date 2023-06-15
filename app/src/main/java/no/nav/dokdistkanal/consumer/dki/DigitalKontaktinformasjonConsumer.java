package no.nav.dokdistkanal.consumer.dki;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.azure.TokenConsumer;
import no.nav.dokdistkanal.azure.TokenResponse;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.DkifResponseTo;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerRequest;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.FunctionalUtils.createHeaders;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer implements DigitalKontaktinformasjon {

	public static final String INGEN_KONTAKTINFORMASJON_FEILMELDING = "person_ikke_funnet";

	private final RestTemplate restTemplate;
	private final String dkiUrl;
	private final String dkiScope;
	private final TokenConsumer tokenConsumer;
	private final DigitalKontaktinfoMapper digitalKontaktinfoMapper = new DigitalKontaktinfoMapper();


	public DigitalKontaktinformasjonConsumer(RestTemplateBuilder restTemplateBuilder,
											 @Value("${digdir_krr_proxy_url}") String dkiUrl,
											 @Value("${digdir_krr_proxy_scope}") String dkiScope,
											 TokenConsumer tokenConsumer) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.dkiUrl = dkiUrl;
		this.dkiScope = dkiScope;
		this.tokenConsumer = tokenConsumer;
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class, noRetryFor = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final boolean inkluderSikkerDigitalPost) {
		TokenResponse clientCredentialToken = tokenConsumer.getClientCredentialToken(dkiScope);
		HttpHeaders headers = createHeaders(clientCredentialToken.getAccess_token());

		final String fnrTrimmed = personidentifikator.trim();
		PostPersonerRequest postPersonRequest = PostPersonerRequest.builder().personidenter(List.of(fnrTrimmed)).build();
		HttpEntity<String> request = new HttpEntity(postPersonRequest, headers);

		try {
			DkifResponseTo response = restTemplate.postForEntity(dkiUrl + "/rest/v1/personer?inkluderSikkerDigitalPost=" + inkluderSikkerDigitalPost, request, DkifResponseTo.class).getBody();
			if (isValidRespons(response, fnrTrimmed)) {
				return digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(response.getPersoner().get(fnrTrimmed));
			} else {
				String errorMsg = getErrorMsg(response, fnrTrimmed);

				if (errorMsg != null && errorMsg.contains(INGEN_KONTAKTINFORMASJON_FEILMELDING)) {
					return null;
				} else {
					throw new DigitalKontaktinformasjonV2FunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s",
							errorMsg));
				}
			}
		} catch (HttpClientErrorException e) {
			throw new DigitalKontaktinformasjonV2FunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DigitalKontaktinformasjonV2TechnicalException(format("Teknisk feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		}
	}

	private boolean isValidRespons(DkifResponseTo response, String fnr) {
		return response != null && response.getPersoner() != null && response.getPersoner().get(fnr) != null;
	}

	private String getErrorMsg(DkifResponseTo response, String fnr) {
		if (response == null || response.getFeil() == null) {
			return null;
		} else {
			return response.getFeil().get(fnr);
		}
	}
}
