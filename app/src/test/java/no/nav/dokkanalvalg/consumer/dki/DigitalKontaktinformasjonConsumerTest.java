package no.nav.dokkanalvalg.consumer.dki;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokkanalvalg.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokkanalvalg.exceptions.DokKanalvalgFunctionalException;
import no.nav.dokkanalvalg.exceptions.DokKanalvalgSecurityException;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.KontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.Sikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Epostadresse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Kontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Mobiltelefonnummer;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentDigitalKontaktinformasjonRequest;
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
	private final static String RESERVASJON = "Reservert";

	private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1 = mock(DigitalKontaktinformasjonV1.class);
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = new DigitalKontaktinformasjonConsumer(digitalKontaktinformasjonV1);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldHentDKIOK() throws Exception {
		when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(HentDigitalKontaktinformasjonRequest.class))).thenReturn(createResponse());

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");

		assertThat(digitalKontaktinformasjonTo.getEpostadresse(), is(EPOSTADRESSE));
		assertThat(digitalKontaktinformasjonTo.getMobiltelefon(), is(MOBIL));
		assertThat(digitalKontaktinformasjonTo.getReservasjon(), is(RESERVASJON));
	}

	@Test
	public void shouldReturnNullWhenRequestEmpty() throws Exception {
		HentDigitalKontaktinformasjonResponse response = createResponse();
		response.setDigitalKontaktinformasjon(null);
		when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(HentDigitalKontaktinformasjonRequest.class))).thenReturn(response);

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");

		assertThat(digitalKontaktinformasjonTo, nullValue());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenDKIIkkeFunnet() throws Exception {
		when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(HentDigitalKontaktinformasjonRequest.class)))
				.thenThrow(new HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet("Finner ikke konraktinfo", new KontaktinformasjonIkkeFunnet()));

		expectedException.expect(DokKanalvalgFunctionalException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke kontaktinformasjon for person med ident:" + FNR+ ", message=Finner ikke konraktinfo");
		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenPersonIkkeFunnet() throws Exception {
		when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(HentDigitalKontaktinformasjonRequest.class)))
				.thenThrow(new HentDigitalKontaktinformasjonPersonIkkeFunnet("Finner ikke person", new PersonIkkeFunnet()));
		expectedException.expect(DokKanalvalgFunctionalException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke person med ident:" + FNR + ", message=Finner ikke person");

		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenSikkerhetsbegrensning() throws Exception {
		when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(HentDigitalKontaktinformasjonRequest.class)))
				.thenThrow(new HentDigitalKontaktinformasjonSikkerhetsbegrensing("Ingen adgang", new Sikkerhetsbegrensing()));
		expectedException.expect(DokKanalvalgSecurityException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=Ingen adgang");

		digitalKontaktinformasjonConsumer.hentDigitalKontaktinformasjon(FNR, "");
	}

	private HentDigitalKontaktinformasjonResponse createResponse() {
		Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
		Epostadresse epostadresse = new Epostadresse();
		epostadresse.setValue(EPOSTADRESSE);
		kontaktinformasjon.setEpostadresse(epostadresse);
		Mobiltelefonnummer mobiltelefonnummer = new Mobiltelefonnummer();
		mobiltelefonnummer.setValue(MOBIL);
		kontaktinformasjon.setMobiltelefonnummer(mobiltelefonnummer);
		kontaktinformasjon.setPersonident(FNR);
		kontaktinformasjon.setReservasjon(RESERVASJON);
		HentDigitalKontaktinformasjonResponse response = new HentDigitalKontaktinformasjonResponse();
		response.setDigitalKontaktinformasjon(kontaktinformasjon);
		return response;
	}
}
