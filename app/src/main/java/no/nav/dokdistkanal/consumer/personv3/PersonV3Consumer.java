package no.nav.dokdistkanal.consumer.personv3;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_DOKDIST;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.PERSONV3;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.personv3.to.PersonV3To;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Joakim Bjørnstad, Jbit AS
 * @author Ketill Fenne, Visma Consulting AS
 */
@Slf4j
@Service
public class PersonV3Consumer {
	private final PersonV3 personV3;
	private Histogram.Timer requestTimer;

	public static final String HENT_PERSON = "hentPerson";

	@Inject
	public PersonV3Consumer(PersonV3 personV3) {
		this.personV3 = personV3;
	}

	@Cacheable(value = HENT_PERSON, key = "#personidentifikator+'-personV3'")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public PersonV3To hentPerson(final String personidentifikator, final String consumerId) throws DokDistKanalTechnicalException, DokDistKanalFunctionalException, DokDistKanalSecurityException {

		requestCounter.labels(HENT_PERSON, CACHE_COUNTER, consumerId, CACHE_MISS).inc();

		HentPersonRequest request = mapHentPersonRequest(personidentifikator);
		HentPersonResponse response;

		try {
			requestTimer = requestLatency.labels(LABEL_DOKDIST, PERSONV3, HENT_PERSON).startTimer();
			response = personV3.hentPerson(request);
		} catch (HentPersonPersonIkkeFunnet hentPersonPersonIkkeFunnet) {
			return null;
//			throw new DokDistKanalFunctionalException("PersonV3.hentPerson fant ikke person med angitt ident, message=" + hentPersonPersonIkkeFunnet
//					.getMessage(), hentPersonPersonIkkeFunnet);
		} catch (HentPersonSikkerhetsbegrensning hentPersonSikkerhetsbegrensning) {
			throw new DokDistKanalSecurityException("PersonV3.hentPerson feiler på grunn av sikkerhetsbegresning. ConsumerId=" + consumerId + ", message=" + hentPersonSikkerhetsbegrensning
					.getMessage(), hentPersonSikkerhetsbegrensning);
		} catch (Exception e) {
			throw new DokDistKanalTechnicalException("Noe gikk galt i kall til PersonV3.hentPerson. ConsumerId=" + consumerId + ", message=" + e
					.getMessage());
		} finally {
			requestTimer.observeDuration();
		}
		if (response != null && response.getPerson() != null) {
			return mapTo((Bruker) response.getPerson());
		}
		return null;
	}

	private HentPersonRequest mapHentPersonRequest(String personidentifikator) {

		Personidenter personidenter = new Personidenter();
		if (StringUtils.startsWithAny(personidentifikator, "0", "1", "2", "3")) {
			personidenter.setValue("FNR");
		} else {
			personidenter.setValue("DNR");
		}

		NorskIdent norskIdent = new NorskIdent();
		norskIdent.setType(personidenter);
		norskIdent.setIdent(personidentifikator);

		PersonIdent personIdent = new PersonIdent();
		personIdent.setIdent(norskIdent);
		HentPersonRequest request = new HentPersonRequest();
		request.setAktoer(personIdent);
		return request;
	}

	private PersonV3To mapTo(Bruker bruker) {
		XMLGregorianCalendar brukerFoedselssdato = bruker.getFoedselsdato() == null ? null : bruker.getFoedselsdato().getFoedselsdato();
		XMLGregorianCalendar brukerDoedsdato = bruker.getDoedsdato() == null ? null : bruker.getDoedsdato().getDoedsdato();

		return PersonV3To.builder()
				.doedsdato(brukerDoedsdato == null ? null : brukerDoedsdato.toGregorianCalendar().toZonedDateTime().toLocalDate())
				.foedselsdato(brukerFoedselssdato == null ? null : brukerFoedselssdato.toGregorianCalendar().toZonedDateTime().toLocalDate())
				.build();
	}
}
