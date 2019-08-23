package no.nav.dokdistkanal.consumer.personv3;

import static no.nav.dokdistkanal.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.personv3.to.PersonV3To;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
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

	public static final String HENT_PERSON = "hentPerson";

	@Inject
	public PersonV3Consumer(PersonV3 personV3) {
		this.personV3 = personV3;
	}

	@Cacheable(value = HENT_PERSON, key = "#personidentifikator+'-personV3'")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_CODE, HENT_PERSON}, percentiles = {0.5, 0.95}, histogram = true)
	public PersonV3To hentPerson(final String personidentifikator, final String consumerId) {

		requestCounter.labels(HENT_PERSON, CACHE_COUNTER, consumerId, CACHE_MISS).inc();

		HentPersonRequest request = mapHentPersonRequest(personidentifikator);
		HentPersonResponse response;

		try {
			response = personV3.hentPerson(request);
		} catch (HentPersonPersonIkkeFunnet hentPersonPersonIkkeFunnet) {
			return null;
		} catch (HentPersonSikkerhetsbegrensning hentPersonSikkerhetsbegrensning) {
			throw new DokDistKanalSecurityException("PersonV3.hentPerson feiler på grunn av sikkerhetsbegresning. ConsumerId=" + consumerId + ", message=" + hentPersonSikkerhetsbegrensning
					.getMessage(), hentPersonSikkerhetsbegrensning);
		} catch (Exception e) {
			throw new PersonV3TechnicalException("Noe gikk galt i kall til PersonV3.hentPerson. ConsumerId=" + consumerId +
					", message=" + e.getMessage(), e);
		}
		if (response != null && response.getPerson() != null) {
			return mapTo(response.getPerson());
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

	private PersonV3To mapTo(Person person) {
		if (person instanceof Bruker) {
			return mapToBruker((Bruker) person);
		} else {
			return mapToPerson(person);
		}
	}

	private PersonV3To mapToPerson(Person person) {
		XMLGregorianCalendar brukerFoedselssdato = getFoedselssdato(person);
		XMLGregorianCalendar brukerDoedsdato = getDoedsdato(person);
		return createTo(brukerFoedselssdato, brukerDoedsdato);
	}

	private PersonV3To mapToBruker(Bruker bruker) {
		XMLGregorianCalendar brukerFoedselssdato = getFoedselssdato(bruker);
		XMLGregorianCalendar brukerDoedsdato = getDoedsdato(bruker);
		return createTo(brukerFoedselssdato, brukerDoedsdato);
	}

	private XMLGregorianCalendar getFoedselssdato(Person person) {
		return person.getFoedselsdato() == null ? null : person.getFoedselsdato().getFoedselsdato();
	}

	private XMLGregorianCalendar getDoedsdato(Person person) {
		return person.getDoedsdato() == null ? null : person.getDoedsdato().getDoedsdato();
	}

	private PersonV3To createTo(XMLGregorianCalendar brukerFoedselssdato, XMLGregorianCalendar brukerDoedsdato) {
		return PersonV3To.builder()
				.doedsdato(brukerDoedsdato == null ? null : brukerDoedsdato.toGregorianCalendar().toZonedDateTime().toLocalDate())
				.foedselsdato(brukerFoedselssdato == null ? null : brukerFoedselssdato.toGregorianCalendar().toZonedDateTime().toLocalDate())
				.build();
	}
}
