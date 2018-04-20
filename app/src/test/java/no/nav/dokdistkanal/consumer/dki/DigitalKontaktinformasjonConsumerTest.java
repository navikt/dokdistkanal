package no.nav.dokdistkanal.consumer.dki;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadressePersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadresseSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.KontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.Sikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Epostadresse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Kontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Mobiltelefonnummer;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.SikkerDigitalKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentSikkerDigitalPostadresseRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentSikkerDigitalPostadresseResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DigitalKontaktinformasjonConsumerTest {
	private final static String FNR = "***gammelt_fnr***";
	private final static String EPOSTADRESSE = "adresse@test.no";
	private final static String MOBIL = "123 45 678";
	private final static boolean RESERVASJON = true;

	private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1 = mock(DigitalKontaktinformasjonV1.class);
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = new DigitalKontaktinformasjonConsumer(digitalKontaktinformasjonV1);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldHentDKIOK() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class))).thenReturn(createResponse());

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");

		assertThat(digitalKontaktinformasjonTo.getEpostadresse(), is(EPOSTADRESSE));
		assertThat(digitalKontaktinformasjonTo.getMobiltelefonnummer(), is(MOBIL));
		assertThat(digitalKontaktinformasjonTo.isReservasjon(), is(RESERVASJON));
	}

	@Test
	public void shouldReturnNullWhenRequestEmpty() throws Exception {
		HentSikkerDigitalPostadresseResponse response = createResponse();
		response.setSikkerDigitalKontaktinformasjon(null);
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class))).thenReturn(response);

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");

		assertThat(digitalKontaktinformasjonTo, nullValue());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenDKIIkkeFunnet() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet("Finner ikke konraktinfo", new KontaktinformasjonIkkeFunnet()));

		expectedException.expect(DokDistKanalFunctionalException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke kontaktinformasjon for person med ident:" + FNR+ ", message=Finner ikke konraktinfo");
		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenPersonIkkeFunnet() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new HentSikkerDigitalPostadressePersonIkkeFunnet("Finner ikke person", new PersonIkkeFunnet()));
		expectedException.expect(DokDistKanalFunctionalException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke person med ident:" + FNR + ", message=Finner ikke person");

		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenSikkerhetsbegrensning() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new HentSikkerDigitalPostadresseSikkerhetsbegrensing("Ingen adgang", new Sikkerhetsbegrensing()));
		expectedException.expect(DokDistKanalSecurityException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=Ingen adgang");

		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new RuntimeException("Runtime Exception"));
		expectedException.expect(DokDistKanalTechnicalException.class);
		expectedException.expectMessage("Noe gikk galt i kall til DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon. message=Runtime Exception");

		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}
	private HentSikkerDigitalPostadresseResponse createResponse() {
		Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
		Epostadresse epostadresse = new Epostadresse();
		epostadresse.setValue(EPOSTADRESSE);
		kontaktinformasjon.setEpostadresse(epostadresse);
		Mobiltelefonnummer mobiltelefonnummer = new Mobiltelefonnummer();
		mobiltelefonnummer.setValue(MOBIL);
		kontaktinformasjon.setMobiltelefonnummer(mobiltelefonnummer);
		kontaktinformasjon.setPersonident(FNR);
		kontaktinformasjon.setReservasjon("true");
		SikkerDigitalKontaktinformasjon sikkerDigitalKontaktinformasjon = new SikkerDigitalKontaktinformasjon();
		sikkerDigitalKontaktinformasjon.setDigitalKontaktinformasjon(kontaktinformasjon);
		HentSikkerDigitalPostadresseResponse response = new HentSikkerDigitalPostadresseResponse();
		response.setSikkerDigitalKontaktinformasjon(sikkerDigitalKontaktinformasjon);
		return response;
	}
}
