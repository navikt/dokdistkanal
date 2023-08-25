package no.nav.dokdistkanal.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.constants.MDCConstants;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.AltinnServiceOwnerConsumer;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.pdl.HentPersoninfo;
import no.nav.dokdistkanal.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.util.stream.Stream;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.common.MottakerTypeCode.ORGANISASJON;
import static no.nav.dokdistkanal.common.MottakerTypeCode.PERSON;
import static no.nav.dokdistkanal.common.MottakerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistkanal.service.DokDistKanalService.BEGRENSET_INNSYN_TEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
public class DokDistKanalServiceTest {

	private final static String FNR = "12345678931";
	private final static String DOKUMENTTYPEID = "000096";
	private final static String DOKUMENTTYPEID_AARSOPPGAVE = "000053";
	private final static String EPOSTADRESSE = "adresse@test.no";
	private final static String MOBIL = "123 45 678";
	private final static boolean SERTIFIKAT = true;
	private final static String LEVERANDORADRESSE = "Leverandøradresse";
	private final static String BRUKERADRESSE = "Brukeradresse";
	private final static String BRUKERID = "55443322110";
	private final static String ANNEN_BRUKERID = "01122334455";
	private final static Boolean ER_ARKIVERT_FALSE = FALSE;
	private final static Boolean ER_ARKIVERT_TRUE = TRUE;
	private final static String CONSUMER_ID = "srvdokdistfordeling";
	public static final String BRUKER_LOGGET = "Bruker har logget på med nivaa4 de siste 18 mnd";
	public static final String BRUKER_IKKE_LOGGET = "Bruker har ikke logget på med nivaa4 de siste 18 mnd";
	public static final String TEMA = "PEN";

	private ListAppender<ILoggingEvent> logWatcher;

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer = mock(DokumentTypeInfoConsumer.class);
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer = mock(SikkerhetsnivaaConsumer.class);
	private final AltinnServiceOwnerConsumer altinnServiceOwnerConsumer = mock(AltinnServiceOwnerConsumer.class);
	private DokDistKanalService service;
	private final PdlGraphQLConsumer pdlGraphQLConsumer = mock(PdlGraphQLConsumer.class);
	private final MeterRegistry registry = new SimpleMeterRegistry();

	@BeforeEach
	public void setUp() {
		logWatcher = new ListAppender<>();
		logWatcher.start();
		((Logger) getLogger(DokDistKanalService.class)).addAppender(logWatcher);
		MDC.put(MDCConstants.CONSUMER_ID, CONSUMER_ID);
		service = new DokDistKanalService(dokumentTypeInfoConsumer, digitalKontaktinformasjonConsumer, sikkerhetsnivaaConsumer,
				registry, pdlGraphQLConsumer, altinnServiceOwnerConsumer);

		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString()))
				.thenReturn(new DokumentTypeInfoTo("JOARK", null, TRUE));

		HentPersoninfo personInfoAttenAar = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(personInfoAttenAar);

		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder()
				.harLoggetPaaNivaa4(true)
				.personIdent(FNR)
				.build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);
	}

	@AfterEach
	public void tearDown() {
		((Logger) LoggerFactory.getLogger(DokDistKanalService.class)).detachAndStopAllAppenders();
	}

	@ParameterizedTest
	@MethodSource("begrensetInnsynTemaFactory")
	public void shouldSendTilPrintNaarTemaBegrensetInnsyn(String tema) {
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.reservasjon(FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().tema(tema).brukerId(BRUKERID)
				.dokumentTypeId(DOKUMENTTYPEID_AARSOPPGAVE)
				.mottakerId(ANNEN_BRUKERID)
				.build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(getLogMessage()).contains("til PRINT: Tema har begrenset innsyn");

	}

	private String getLogMessage() {
		return logWatcher.list.stream().map(ILoggingEvent::getMessage)
				.findAny().orElse(null);
	}

	static Stream<String> begrensetInnsynTemaFactory() {
		return BEGRENSET_INNSYN_TEMA.stream();
	}

	@Test
	public void shouldSettPrintNavNaarIngenArkivering() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("INGEN", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldSetKanalDittNavNaarIngenArkiveringLP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", LOKAL_PRINT.toString(), TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(LOKAL_PRINT, serviceResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldSetKanalIngenDistribusjonNaarIngenDistribusjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", INGEN_DISTRIBUSJON.toString(), TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(INGEN_DISTRIBUSJON, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(INGEN_DISTRIBUSJON) + "Predefinert distribusjonskanal er Ingen Distribusjon");
	}

	@Test
	public void shouldSetKanalTrygderettenNaarPredefinertTrygderetten() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", TRYGDERETTEN.toString(), TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(TRYGDERETTEN, serviceResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldSetKanalPrintNaarOrganisasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder()
				.brukerId(BRUKERID)
				.mottakerType(ORGANISASJON)
				.mottakerId(BRUKERID)
				.build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Mottaker er av typen ORGANISASJON");

	}

	@Test
	public void shouldSetKanalPrintNaarSamhandler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder()
				.mottakerType(SAMHANDLER_HPR)
				.build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Mottaker er av typen SAMHANDLER_HPR");

	}

	@Test
	public void shouldSetKanalPrintNaarIngenPerson() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(null);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Finner ikke personen i PDL");

	}

	@Test
	public void shouldSetKanalDittNavNaarPersonDoed() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.doedsdato(LocalDate.now())
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Personen er død");

	}

	@Test
	public void shouldSetKanalPrintNaarPersonManglerFoedselsdato() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.doedsdato(null)
				.foedselsdato(null)
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Personens alder er ukjent");

	}


	@Test
	public void shouldSetKanalPrintNaarPersonUnder18() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(17).minusMonths(11))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Personen må være minst 18 år gammel");

	}

	@Test
	public void shouldSetKanalPrintNaarDKIMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(null);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Finner ikke Digital kontaktinformasjon");

	}

	@Test
	public void shouldSetKanalPrintNaarReservasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(TRUE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL)
				.build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Bruker har reservert seg");

	}

	@Test
	public void shouldSetKanalPrintNaarMottakerIdIkkeErBrukerId() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.gyldigSertifikat(TRUE)
				.reservasjon(FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.mottakerId(ANNEN_BRUKERID)
				.build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Bruker og mottaker er forskjellige");

	}

	@Test
	public void shouldSetKanalDittNavNaarMottakerIdIkkeErBrukerIdAndDokumentTypeIdIsAarsoppgave() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.reservasjon(FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.dokumentTypeId(DOKUMENTTYPEID_AARSOPPGAVE)
				.mottakerId(ANNEN_BRUKERID)
				.build());

		assertEquals(DITT_NAV, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(DITT_NAV) + BRUKER_LOGGET);

	}

	@Test
	public void shouldSetKanalSDPNaarAltOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL)
				.build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(SDP, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(SDP) + "Sertifikat, LeverandørAddresse og BrukerAdresse har verdi.");

	}

	@Test
	public void shouldSetKanalSDPNaarBrukerIkkeVarslesMenEpostOgMobilErTomme() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Epostadresse og mobiltelefon - feltene er tomme");
	}

	@Test
	public void shouldSetKanalPrintNaarMobilOgEpostMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(null)
				.mobiltelefonnummer(null)
				.build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi");
	}

	@Test
	public void shouldSetKanalDittNavNaarPaalogginsnivaa4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DigitalKontaktinformasjonTo dkiResponse = digitalKontaktinformasjonNoReservationInvalidCertificate();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DITT_NAV, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(DITT_NAV) + BRUKER_LOGGET);

	}

	@Test
	public void shouldSetKanalPrintNaarIkkePaalogginsnivaa4OgIkkeSDPOgArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL)
				.build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder()
				.harLoggetPaaNivaa4(false)
				.personIdent(FNR)
				.build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + BRUKER_IKKE_LOGGET);

	}

	@Test
	public void shouldSetKanalPrintNaarPaalogginsnivaaIkkeFunnet4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DigitalKontaktinformasjonTo dkiResponse = digitalKontaktinformasjonNoReservationInvalidCertificate();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = null;
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Paaloggingsnivaa ikke tilgjengelig");

	}

	@Test
	public void shouldSetKanaPrintNaarPaalogginsnivaa4OgIkkeSDPOgIkkeArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DigitalKontaktinformasjonTo dkiResponse = digitalKontaktinformasjonNoReservationInvalidCertificate();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().erArkivert(ER_ARKIVERT_FALSE)
				.build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(TestUtils.getLogMessage(logWatcher)).contains(createLogMelding(PRINT) + "Dokumentet er ikke arkivert");

	}

	private static DigitalKontaktinformasjonTo digitalKontaktinformasjonNoReservationInvalidCertificate() {
		return DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.reservasjon(FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL)
				.build();
	}

	private DokDistKanalRequest.DokDistKanalRequestBuilder baseDokDistKanalRequestBuilder() {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(DOKUMENTTYPEID)
				.mottakerId(FNR)
				.mottakerType(PERSON)
				.brukerId(FNR)
				.erArkivert(ER_ARKIVERT_TRUE)
				.tema(TEMA);
	}

	private static String createLogMelding(DistribusjonKanalCode kanalCode) {
		return format("BestemKanal: Sender melding fra %s (Tema=%s) til %s: ", CONSUMER_ID, TEMA, kanalCode);
	}

}

