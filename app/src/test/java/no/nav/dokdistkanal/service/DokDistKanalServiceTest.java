package no.nav.dokdistkanal.service;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.common.MottakerTypeCode;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.consumer.tps.TpsConsumer;
import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.util.LogbackCapturingAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@RunWith(MockitoJUnitRunner.class)
public class DokDistKanalServiceTest {

	private final static String FNR = "12345678931";
	private final static String DOKUMENTTYPEID = "DokumentType";
	private final static String DOKUMENTTYPEID_AARSOPPGAVE = "000053";
	private final static String EPOSTADRESSE = "adresse@test.no";
	private final static String MOBIL = "123 45 678";
	private final static boolean SERTIFIKAT = true;
	private final static String LEVERANDORADRESSE = "Leverandøradresse";
	private final static String BRUKERADRESSE = "Brukeradresse";
	private final static String BRUKERID = "55443322110";
	private final static String ANNEN_BRUKERID = "01122334455";
	private final static Boolean ER_ARKIVERT_FALSE = Boolean.FALSE;
	private final static Boolean ER_ARKIVERT_TRUE = Boolean.TRUE;

	private LogbackCapturingAppender capture;

	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer = mock(DokumentTypeInfoConsumer.class);
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
	private SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer = mock(SikkerhetsnivaaConsumer.class);
	private MeterRegistry registry;
	private DokDistKanalService service;
	private TpsConsumer tpsConsumer = mock(TpsConsumer.class);

	@Mock
	private Appender mockAppender;

	@Captor
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

	@Before
	public void setUp() {
		registry = new SimpleMeterRegistry();
		service = new DokDistKanalService(dokumentTypeInfoConsumer, digitalKontaktinformasjonConsumer, sikkerhetsnivaaConsumer, registry, tpsConsumer);
	}

	@After
	public void tearDown() {
		final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.detachAppender(mockAppender);
	}

	@Test
	public void shouldSettPrintNavNaarIngenArkivering() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("INGEN", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldSetKanalDittNavNaarIngenArkiveringLP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", DistribusjonKanalCode.LOKAL_PRINT.toString(), Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.LOKAL_PRINT, serviceResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldSetKanalIngenDistribusjonNaarIngenDistribusjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", DistribusjonKanalCode.INGEN_DISTRIBUSJON.toString(), Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.INGEN_DISTRIBUSJON, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til INGEN_DISTRIBUSJON: Predefinert distribusjonskanal er Ingen Distribusjon"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalTrygderettenNaarPredefinertTrygderetten() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", DistribusjonKanalCode.TRYGDERETTEN.toString(), Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.TRYGDERETTEN, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til TRYGDERETTEN: Predefinert distribusjonskanal er Trygderetten"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarOrganisasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.mottakerType(MottakerTypeCode.ORGANISASJON).mottakerId(BRUKERID).build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Mottaker er av typen ORGANISASJON"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarSamhandler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().mottakerType(MottakerTypeCode.SAMHANDLER_HPR)
				.build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Mottaker er av typen SAMHANDLER_HPR"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarIngenPerson() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = null;
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Finner ikke personen i TPS"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarPersonDoed() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder().doedsdato(LocalDate.now()).build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personen er død"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarPersonManglerFoedselsdato() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.doedsdato(null)
				.foedselsdato(null)
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personens alder er ukjent"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}


	@Test
	public void shouldSetKanalPrintNaarPersonUnder18() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(17).minusMonths(11))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personen må være minst 18 år gammel"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarDKIMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = null;
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Finner ikke Digital kontaktinformasjon"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarReservasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.TRUE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker har reservert seg"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarMottakerIdIkkeErBrukerId() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.reservasjon(Boolean.FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.mottakerId(ANNEN_BRUKERID)
				.build());

		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker og mottaker er forskjellige"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarMottakerIdIkkeErBrukerIdAndDokumentTypeIdIsAarsoppgave() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.reservasjon(Boolean.FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.dokumentTypeId(DOKUMENTTYPEID_AARSOPPGAVE)
				.mottakerId(ANNEN_BRUKERID)
				.build());

		assertEquals(DistribusjonKanalCode.DITT_NAV, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til DITT_NAV: Bruker har logget på med nivaa4 de siste 18 mnd"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarAltOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.SDP, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til SDP: Sertifikat, LeverandørAddresse og BrukerAdresse har verdi."));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarBrukerIkkeVarslesMenEpostOgMobilErTomme() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Epostadresse og mobiltelefon - feltene er tomme"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarMobilOgEpostMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(null)
				.mobiltelefonnummer(null).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarPaalogginsnivaa4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.DITT_NAV, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til DITT_NAV: Bruker har logget på med nivaa4 de siste 18 mnd"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarIkkePaalogginsnivaa4OgIkkeSDPOgArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(false).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker har ikke logget på med nivaa4 de siste 18 mnd"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarPaalogginsnivaaIkkeFunnet4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = null;
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Paaloggingsnivaa ikke tilgjengelig"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanaPrintNaarPaalogginsnivaa4OgIkkeSDPOgIkkeArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		TpsHentPersoninfoForIdentTo personinfoTo = TpsHentPersoninfoForIdentTo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(tpsConsumer.tpsHentPersoninfoForIdent(anyString())).thenReturn(personinfoTo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().erArkivert(ER_ARKIVERT_FALSE)
				.build());
		assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Dokumentet er ikke arkivert"));
		assertThat(capture.getCapturedLogLevel(), is(Level.INFO));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	private DokDistKanalRequest.DokDistKanalRequestBuilder baseDokDistKanalRequestBuilder() {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(DOKUMENTTYPEID)
				.mottakerId(FNR)
				.mottakerType(MottakerTypeCode.PERSON)
				.brukerId(FNR)
				.erArkivert(ER_ARKIVERT_TRUE);
	}

}

