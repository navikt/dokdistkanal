package no.nav.dokdistkanal.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.common.MottakerTypeCode;
import no.nav.dokdistkanal.constants.MDCConstants;
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
import no.nav.dokdistkanal.util.LogbackCapturingAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDate;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.service.DokDistKanalService.LOG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
	private final static Boolean ER_ARKIVERT_TRUE = TRUE;
	private final static String CONSUMER_ID = "srvdokdistfordeling";
	public static final String BRUKER_LOGGET = "Bruker har logget på med nivaa4 de siste 18 mnd";
	public static final String BRUKER_IKKE_LOGGET = "Bruker har ikke logget på med nivaa4 de siste 18 mnd";
    public static final String TEMA = "PEN";

    private LogbackCapturingAppender capture;

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer = mock(DokumentTypeInfoConsumer.class);
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer = mock(SikkerhetsnivaaConsumer.class);
	private DokDistKanalService service;
	private final PdlGraphQLConsumer pdlGraphQLConsumer = mock(PdlGraphQLConsumer.class);
	private MeterRegistry registry;

	@BeforeEach
	public void setUp() {
		MDC.put(MDCConstants.CONSUMER_ID, CONSUMER_ID);
		registry = new SimpleMeterRegistry();
		service = new DokDistKanalService(dokumentTypeInfoConsumer, digitalKontaktinformasjonConsumer, sikkerhetsnivaaConsumer, registry, pdlGraphQLConsumer);
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
		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", DistribusjonKanalCode.LOKAL_PRINT.toString(), TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.LOKAL_PRINT, serviceResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldSetKanalIngenDistribusjonNaarIngenDistribusjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", INGEN_DISTRIBUSJON.toString(), TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(INGEN_DISTRIBUSJON, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, INGEN_DISTRIBUSJON, TEMA) + "Predefinert distribusjonskanal er Ingen Distribusjon"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalTrygderettenNaarPredefinertTrygderetten() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", TRYGDERETTEN.toString(), TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(TRYGDERETTEN, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, TRYGDERETTEN, TEMA) + "Predefinert distribusjonskanal er Trygderetten"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarOrganisasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.mottakerType(MottakerTypeCode.ORGANISASJON).mottakerId(BRUKERID).build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Mottaker er av typen ORGANISASJON"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarSamhandler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().mottakerType(MottakerTypeCode.SAMHANDLER_HPR)
				.build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Mottaker er av typen SAMHANDLER_HPR"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarIngenPerson() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersonResponse = null;
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersonResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Finner ikke personen i PDL"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarPersonDoed() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder().doedsdato(LocalDate.now()).build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Personen er død"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarPersonManglerFoedselsdato() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.doedsdato(null)
				.foedselsdato(null)
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Personens alder er ukjent"));
		LogbackCapturingAppender.Factory.cleanUp();
	}


	@Test
	public void shouldSetKanalPrintNaarPersonUnder18() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(17).minusMonths(11))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Personen må være minst 18 år gammel"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarDKIMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DigitalKontaktinformasjonTo dkiResponse = null;
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Finner ikke Digital kontaktinformasjon"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarReservasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(TRUE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Bruker har reservert seg"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarMottakerIdIkkeErBrukerId() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.reservasjon(Boolean.FALSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(true).personIdent(FNR).build();
		when(sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(anyString())).thenReturn(sikkerhetsnivaaTo);

		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().brukerId(BRUKERID)
				.mottakerId(ANNEN_BRUKERID)
				.build());

		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Bruker og mottaker er forskjellige"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarMottakerIdIkkeErBrukerIdAndDokumentTypeIdIsAarsoppgave() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo personinfoTo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(personinfoTo);
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
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, DITT_NAV, TEMA) + BRUKER_LOGGET));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarAltOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBIL).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(DistribusjonKanalCode.SDP, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, SDP, TEMA) + "Sertifikat, LeverandørAddresse og BrukerAdresse har verdi."));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalSDPNaarBrukerIkkeVarslesMenEpostOgMobilErTomme() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Epostadresse og mobiltelefon - feltene er tomme"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarMobilOgEpostMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, TRUE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
		DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
				.brukerAdresse(BRUKERADRESSE)
				.gyldigSertifikat(SERTIFIKAT)
				.reservasjon(Boolean.FALSE)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(null)
				.mobiltelefonnummer(null).build();
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
		DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalDittNavNaarPaalogginsnivaa4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
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
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, DITT_NAV, TEMA) + BRUKER_LOGGET));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarIkkePaalogginsnivaa4OgIkkeSDPOgArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
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
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + BRUKER_IKKE_LOGGET));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanalPrintNaarPaalogginsnivaaIkkeFunnet4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
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
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Paaloggingsnivaa ikke tilgjengelig"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	@Test
	public void shouldSetKanaPrintNaarPaalogginsnivaa4OgIkkeSDPOgIkkeArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		capture = LogbackCapturingAppender.Factory.weaveInto(LOG);

		DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.FALSE);
		when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
		HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
				.foedselsdato(LocalDate.now().minusYears(18))
				.build();
		when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
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
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(capture.getCapturedLogMessage(), is(createLogMelding(CONSUMER_ID, PRINT, TEMA) + "Dokumentet er ikke arkivert"));
		LogbackCapturingAppender.Factory.cleanUp();
	}

	private DokDistKanalRequest.DokDistKanalRequestBuilder baseDokDistKanalRequestBuilder() {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(DOKUMENTTYPEID)
				.mottakerId(FNR)
				.mottakerType(MottakerTypeCode.PERSON)
				.brukerId(FNR)
				.erArkivert(ER_ARKIVERT_TRUE)
				.tema(TEMA);
	}

	private static final String createLogMelding(String consumerId, DistribusjonKanalCode kanalCode, String tema) {
		return format("BestemKanal: Sender melding fra %s (Tema=%s) til %s: ", consumerId, tema, kanalCode);
	}

}

