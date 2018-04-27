package no.nav.dokdistkanal.consumer.personv3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.personv3.to.PersonV3To;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.feil.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;

public class PersonV3ConsumerTest {
	private static final String FNR = "***gammelt_fnr***";
	private static final LocalDate FOEDSELSDATO = LocalDate.of(1899, 12, 31);
	private static final LocalDate DOEDSDATO = LocalDate.of(1999, 12, 31);
	private static final String PRINCIPAL = "SRVDOKPROD";

	private PersonV3 personV3 = mock(PersonV3.class);
	private PersonV3Consumer personV3Consumer = new PersonV3Consumer(personV3);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldHentPersonOK() throws Exception {
		when(personV3.hentPerson(any(HentPersonRequest.class))).thenReturn(createResponse());

		PersonV3To personV3To = personV3Consumer.hentPerson(FNR, PRINCIPAL);

		assertThat(personV3To.getDoedsdato(), is(DOEDSDATO));
		assertThat(personV3To.getFoedselsdato(), is(FOEDSELSDATO));

	}

	@Test
	public void shouldReturnNullWhenNavnInResponse() throws Exception {
		HentPersonResponse response = createResponse();
		response.setPerson(null);
		when(personV3.hentPerson(any(HentPersonRequest.class))).thenReturn(response);

		PersonV3To personV3To = personV3Consumer.hentPerson(FNR, PRINCIPAL);

		assertThat(personV3To, nullValue());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenPersonIkkeFunnet() throws Exception {
		when(personV3.hentPerson(any(HentPersonRequest.class))).thenThrow(new HentPersonPersonIkkeFunnet("Fant ikke person", new PersonIkkeFunnet()));

		expectedException.expect(DokDistKanalFunctionalException.class);
		expectedException.expectMessage("PersonV3.hentPerson fant ikke person med angitt ident");
		personV3Consumer.hentPerson(FNR, PRINCIPAL);
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenSikkerhetsbegrensning() throws Exception {
		when(personV3.hentPerson(any(HentPersonRequest.class))).thenThrow(new HentPersonSikkerhetsbegrensning("Ingen adgang", new Sikkerhetsbegrensning()));
		expectedException.expect(DokDistKanalSecurityException.class);
		expectedException.expectMessage("PersonV3.hentPerson feiler på grunn av sikkerhetsbegresning. ConsumerId=" + PRINCIPAL + ", message=Ingen adgang");

		personV3Consumer.hentPerson(FNR, PRINCIPAL);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws Exception {
		when(personV3.hentPerson(any(HentPersonRequest.class))).thenThrow(new RuntimeException("Feil oppstått"));
		expectedException.expect(DokDistKanalTechnicalException.class);
		expectedException.expectMessage("Noe gikk galt i kall til PersonV3.hentPerson. ConsumerId=SRVDOKPROD, message=Feil oppstått");

		personV3Consumer.hentPerson(FNR, PRINCIPAL);
	}

	private HentPersonResponse createResponse() throws DatatypeConfigurationException {
		HentPersonResponse response = new HentPersonResponse();
		GregorianCalendar gcalDoedsdato = GregorianCalendar.from(DOEDSDATO.atStartOfDay(ZoneId.systemDefault()));
		Doedsdato doedsdato = new Doedsdato();
		doedsdato.setDoedsdato(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcalDoedsdato));

		GregorianCalendar gcalFoedselsdato = GregorianCalendar.from(FOEDSELSDATO.atStartOfDay(ZoneId.systemDefault()));
		Foedselsdato foedselsdato = new Foedselsdato();
		foedselsdato.setFoedselsdato(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcalFoedselsdato));

		Bruker person = new Bruker();
		person.setDoedsdato(doedsdato);
		person.setFoedselsdato(foedselsdato);
		response.setPerson(person);
		return response;
	}


}
