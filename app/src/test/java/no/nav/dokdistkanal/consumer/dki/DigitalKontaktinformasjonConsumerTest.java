package no.nav.dokdistkanal.consumer.dki;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.metrics.MicrometerMetrics;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadressePersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadresseSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.KontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.feil.Sikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.DigitalPostkasse;
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
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@RunWith(MockitoJUnitRunner.class)
public class DigitalKontaktinformasjonConsumerTest {
	private final static String FNR = "***gammelt_fnr***";
	private final static String EPOSTADRESSE = "adresse@test.no";
	private final static String MOBIL = "123 45 678";
	private final static boolean RESERVASJON = true;
	private final static String LEVERANDORADRESSE = "Leverandøradresse";
	private final static String BRUKERADRESSE = "Brukeradresse";

	private MicrometerMetrics metrics = mock(MicrometerMetrics.class);
	private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1 = mock(DigitalKontaktinformasjonV1.class);
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = new DigitalKontaktinformasjonConsumer(
			digitalKontaktinformasjonV1,
			metrics);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldHentDKI() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class))).thenReturn(createResponse());

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);

		assertThat(digitalKontaktinformasjonTo.getEpostadresse(), is(EPOSTADRESSE));
		assertThat(digitalKontaktinformasjonTo.getMobiltelefonnummer(), is(MOBIL));
		assertThat(digitalKontaktinformasjonTo.isReservasjon(), is(RESERVASJON));
		assertThat(digitalKontaktinformasjonTo.isSertifikat(), is(Boolean.TRUE));
		assertThat(digitalKontaktinformasjonTo.getBrukerAdresse(), is(BRUKERADRESSE));
		assertThat(digitalKontaktinformasjonTo.getLeverandoerAdresse(), is(LEVERANDORADRESSE));
	}

	@Test
	public void shouldHentDKIOld() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class))).thenReturn(createOldResponse());

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);

		assertThat(digitalKontaktinformasjonTo.getEpostadresse(), nullValue());
		assertThat(digitalKontaktinformasjonTo.getMobiltelefonnummer(), nullValue());
		assertThat(digitalKontaktinformasjonTo.isReservasjon(), is(RESERVASJON));
		assertThat(digitalKontaktinformasjonTo.isSertifikat(), is(Boolean.TRUE));
		assertThat(digitalKontaktinformasjonTo.getBrukerAdresse(), is(BRUKERADRESSE));
		assertThat(digitalKontaktinformasjonTo.getLeverandoerAdresse(), is(LEVERANDORADRESSE));
	}


	@Test
	public void shouldReturnNullWhenRequestEmpty() throws Exception {
		HentSikkerDigitalPostadresseResponse response = createResponse();
		response.setSikkerDigitalKontaktinformasjon(null);
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class))).thenReturn(response);

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);

		assertThat(digitalKontaktinformasjonTo, nullValue());
	}

	@Test
	public void shouldReturnNullWhenDKIIkkeFunnet() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet("Finner ikke konraktinfo", new KontaktinformasjonIkkeFunnet()));

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);
		assertThat(digitalKontaktinformasjonTo, nullValue());
	}

	@Test
	public void shouldReturnNullWhenPersonIkkeFunnet() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new HentSikkerDigitalPostadressePersonIkkeFunnet("Finner ikke person", new PersonIkkeFunnet()));

		digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);
		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);
		assertThat(digitalKontaktinformasjonTo, nullValue());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenSikkerhetsbegrensning() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new HentSikkerDigitalPostadresseSikkerhetsbegrensing("Ingen adgang", new Sikkerhetsbegrensing()));
		expectedException.expect(DokDistKanalSecurityException.class);
		expectedException.expectMessage("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=Ingen adgang");

		digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws Exception {
		when(digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(any(HentSikkerDigitalPostadresseRequest.class)))
				.thenThrow(new RuntimeException("Runtime Exception"));
		expectedException.expect(DkifTechnicalException.class);
		expectedException.expectMessage("Noe gikk galt i kall til DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon. message=Runtime Exception");

		digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR);
	}

	private HentSikkerDigitalPostadresseResponse createResponse() throws DatatypeConfigurationException {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		XMLGregorianCalendar gcalNow = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

		Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
		Epostadresse epostadresse = new Epostadresse();
		epostadresse.setValue(EPOSTADRESSE);
		epostadresse.setSistOppdatert(gcalNow);
		kontaktinformasjon.setEpostadresse(epostadresse);
		Mobiltelefonnummer mobiltelefonnummer = new Mobiltelefonnummer();
		mobiltelefonnummer.setSistOppdatert(gcalNow);
		mobiltelefonnummer.setValue(MOBIL);
		kontaktinformasjon.setMobiltelefonnummer(mobiltelefonnummer);
		kontaktinformasjon.setPersonident(FNR);
		kontaktinformasjon.setReservasjon("true");

		DigitalPostkasse digitalPostkasse = new DigitalPostkasse();
		digitalPostkasse.setBrukerAdresse(BRUKERADRESSE);
		digitalPostkasse.setLeverandoerAdresse(LEVERANDORADRESSE);

		SikkerDigitalKontaktinformasjon sikkerDigitalKontaktinformasjon = new SikkerDigitalKontaktinformasjon();
		sikkerDigitalKontaktinformasjon.setSertifikat("mitt sertifikat" .getBytes());
		sikkerDigitalKontaktinformasjon.setDigitalKontaktinformasjon(kontaktinformasjon);
		sikkerDigitalKontaktinformasjon.setSikkerDigitalPostkasse(digitalPostkasse);
		HentSikkerDigitalPostadresseResponse response = new HentSikkerDigitalPostadresseResponse();
		response.setSikkerDigitalKontaktinformasjon(sikkerDigitalKontaktinformasjon);
		return response;
	}


	private HentSikkerDigitalPostadresseResponse createOldResponse() throws DatatypeConfigurationException {
		GregorianCalendar gcal = new GregorianCalendar();
		Calendar cal = Calendar.getInstance();
		cal.set(1999, 01, 01);
		Date date = cal.getTime();
		gcal.setTime(date);
		XMLGregorianCalendar gcalOld = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);

		Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
		Epostadresse epostadresse = new Epostadresse();
		epostadresse.setValue(EPOSTADRESSE);
		epostadresse.setSistOppdatert(gcalOld);
		kontaktinformasjon.setEpostadresse(epostadresse);
		Mobiltelefonnummer mobiltelefonnummer = new Mobiltelefonnummer();
		mobiltelefonnummer.setSistOppdatert(gcalOld);
		mobiltelefonnummer.setValue(MOBIL);
		kontaktinformasjon.setMobiltelefonnummer(mobiltelefonnummer);
		kontaktinformasjon.setPersonident(FNR);
		kontaktinformasjon.setReservasjon("true");

		DigitalPostkasse digitalPostkasse = new DigitalPostkasse();
		digitalPostkasse.setBrukerAdresse(BRUKERADRESSE);
		digitalPostkasse.setLeverandoerAdresse(LEVERANDORADRESSE);

		SikkerDigitalKontaktinformasjon sikkerDigitalKontaktinformasjon = new SikkerDigitalKontaktinformasjon();
		sikkerDigitalKontaktinformasjon.setSertifikat("mitt sertifikat" .getBytes());
		sikkerDigitalKontaktinformasjon.setDigitalKontaktinformasjon(kontaktinformasjon);
		sikkerDigitalKontaktinformasjon.setSikkerDigitalPostkasse(digitalPostkasse);
		HentSikkerDigitalPostadresseResponse response = new HentSikkerDigitalPostadresseResponse();
		response.setSikkerDigitalKontaktinformasjon(sikkerDigitalKontaktinformasjon);
		return response;

	}
}
