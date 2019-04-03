package no.nav.dokdistkanal.service;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.common.MottakerTypeCode;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer;
import no.nav.dokdistkanal.consumer.personv3.to.PersonV3To;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.util.LogbackCapturingAppender;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@RunWith(MockitoJUnitRunner.class)
public class DokDistKanalServiceTest {

	private final static String FNR = "***gammelt_fnr***";
	private final static String DOKUMENTTYPEID = "DokumentType";
	private final static String EPOSTADRESSE = "adresse@test.no";
	private final static String MOBIL = "123 45 678";
	private final static boolean SERTIFIKAT = true;
	private final static String LEVERANDORADRESSE = "Leverandøradresse";
	private final static String BRUKERADRESSE = "Brukeradresse";
	private final static String BRUKERID = "***gammelt_fnr***";
	private final static String ANNEN_BRUKERID = "***gammelt_fnr***";

	private LogbackCapturingAppender capture;

	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer = mock(DokumentTypeInfoConsumer.class);
	private PersonV3Consumer personV3Consumer = mock(PersonV3Consumer.class);
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
	private SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer = mock(SikkerhetsnivaaConsumer.class);

	private DokDistKanalService service = new DokDistKanalService(dokumentTypeInfoConsumer, personV3Consumer, digitalKontaktinformasjonConsumer, sikkerhetsnivaaConsumer);

	@Mock
	private Appender mockAppender;

	@Captor
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

	@After
	public void tearDown() {
		final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.detachAppender(mockAppender);
	}

	@Test
	public void shouldSettPrintNavNaarIngenArkivering() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("INGEN", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
	}

	@Test
	public void shouldSetKanalDittNavNaarIngenArkiveringLP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", DistribusjonKanalCode.LOKAL_PRINT.toString(), Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.LOKAL_PRINT);
	}

	@Test
	public void shouldSetKanalIngenDistribusjonNaarIngenDistribusjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", DistribusjonKanalCode.INGEN_DISTRIBUSJON.toString(), Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.INGEN_DISTRIBUSJON);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til INGEN_DISTRIBUSJON: Predefinert distribusjonskanal er Ingen Distribusjon"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}


	@Test
	public void shouldSetKanalPrintNaarOrganisasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);

		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, BRUKERID, MottakerTypeCode.ORGANISASJON, BRUKERID);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Mottaker er av typen ORGANISASJON"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarSamhandler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);

		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.SAMHANDLER_HPR, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Mottaker er av typen SAMHANDLER_HPR"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarIngenPerson() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = null;
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Finner ikke personen i TPS"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarPersonDoed() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().doedsdato(LocalDate.now()).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personen er død"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarPersonManglerFoedselsdato() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().doedsdato(null).foedselsdato(null).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personens alder er ukjent"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}


	@Test
	public void shouldSetKanalPrintNaarPersonUnder18() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(17).minusMonths(11)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personen må være minst 18 år gammel"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarDKIMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = null;
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Finner ikke Digital kontaktinformasjon"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarReservasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.TRUE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker har reservert seg"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarMottakerIdIkkeErBrukerId() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.reservasjon(Boolean.FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, BRUKERID, MottakerTypeCode.PERSON, ANNEN_BRUKERID);

		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker og mottaker er forskjellige"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarAltOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.SDP);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til SDP: Sertifikat, LeverandørAddresse og BrukerAdresse har verdi."));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarBrukerIkkeVarslesMenEpostOgMobilErTomme() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Epostadresse og mobiltelefon - feltene er tomme"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarMobilOgEpostMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(null)
				.mobiltelefonnummer(null).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarPaalogginsnivaa4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.DITT_NAV);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til DITT_NAV: Bruker har logget på med nivaa4 de siste 18 mnd"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarIkkePaalogginsnivaa4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(false).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker har ikke logget på med nivaa4 de siste 18 mnd"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarPaalogginsnivaaIkkeFunnet4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		PersonV3To personV3To = PersonV3To.builder().foedselsdato(LocalDate.now().minusYears(18)).build();
		when(personV3Consumer.hentPerson(anyString(), anyString())).thenReturn(personV3To);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = null;
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(DOKUMENTTYPEID, FNR, MottakerTypeCode.PERSON, FNR);
		assertEquals(serviceResponse.getDistribusjonsKanal(), DistribusjonKanalCode.PRINT);
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Paaloggingsnivaa ikke tilgjengelig"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

}

