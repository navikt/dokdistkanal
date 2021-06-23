package no.nav.dokdistkanal.service;

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
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.pdl.HentPersoninfo;
import no.nav.dokdistkanal.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.util.LogbackCapturingAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.LoggingEvent;

import java.time.LocalDate;

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
    private final static Boolean ER_ARKIVERT_TRUE = Boolean.TRUE;

    private LogbackCapturingAppender capture;

    private DokumentTypeInfoConsumer dokumentTypeInfoConsumer = mock(DokumentTypeInfoConsumer.class);
    private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
    private SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer = mock(SikkerhetsnivaaConsumer.class);
    private MeterRegistry registry;
    private DokDistKanalService service;
    private PdlGraphQLConsumer pdlGraphQLConsumer = mock(PdlGraphQLConsumer.class);


    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @BeforeEach
    public void setUp() {
        registry = new SimpleMeterRegistry();
        service = new DokDistKanalService(dokumentTypeInfoConsumer, digitalKontaktinformasjonConsumer, sikkerhetsnivaaConsumer, registry, pdlGraphQLConsumer);
    }

    @AfterEach
    public void tearDown() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
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
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarIngenPerson() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
        when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
        HentPersoninfo hentPersonResponse = null;
        when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersonResponse);
        DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Finner ikke personen i PDL"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalDittNavNaarPersonDoed() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
        when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
        HentPersoninfo hentPersoninfo = HentPersoninfo.builder().doedsdato(LocalDate.now()).build();
        when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
        DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personen er død"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarPersonManglerFoedselsdato() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
        when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
        HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
                .doedsdato(null)
                .foedselsdato(null)
                .build();
        when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
        DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personens alder er ukjent"));
        LogbackCapturingAppender.Factory.cleanUp();
    }


    @Test
    public void shouldSetKanalPrintNaarPersonUnder18() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
        when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
        HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
                .foedselsdato(LocalDate.now().minusYears(17).minusMonths(11))
                .build();
        when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
        DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Personen må være minst 18 år gammel"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarDKIMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
        when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
        HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
                .foedselsdato(LocalDate.now().minusYears(18))
                .build();
        when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
        DigitalKontaktinformasjonTo dkiResponse = null;
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
        DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Finner ikke Digital kontaktinformasjon"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarReservasjon() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
        when(dokumentTypeInfoConsumer.hentDokumenttypeInfo(anyString())).thenReturn(response);
        HentPersoninfo hentPersoninfo = HentPersoninfo.builder()
                .foedselsdato(LocalDate.now().minusYears(18))
                .build();
        when(pdlGraphQLConsumer.hentPerson(anyString(), anyString())).thenReturn(hentPersoninfo);
        DigitalKontaktinformasjonTo dkiResponse = DigitalKontaktinformasjonTo.builder()
                .brukerAdresse(BRUKERADRESSE)
                .gyldigSertifikat(SERTIFIKAT)
                .reservasjon(Boolean.TRUE)
                .leverandoerAdresse(LEVERANDORADRESSE)
                .epostadresse(EPOSTADRESSE)
                .mobiltelefonnummer(MOBIL).build();
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString(), anyBoolean())).thenReturn(dkiResponse);
        DokDistKanalResponse serviceResponse = service.velgKanal(baseDokDistKanalRequestBuilder().build());
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker har reservert seg"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarMottakerIdIkkeErBrukerId() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
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

        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker og mottaker er forskjellige"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalDittNavNaarMottakerIdIkkeErBrukerIdAndDokumentTypeIdIsAarsoppgave() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
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
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til DITT_NAV: Bruker har logget på med nivaa4 de siste 18 mnd"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalSDPNaarAltOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
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
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til SDP: Sertifikat, LeverandørAddresse og BrukerAdresse har verdi."));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalSDPNaarBrukerIkkeVarslesMenEpostOgMobilErTomme() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

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
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Epostadresse og mobiltelefon - feltene er tomme"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarMobilOgEpostMangler() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

        DokumentTypeInfoTo response = new DokumentTypeInfoTo("JOARK", null, Boolean.TRUE);
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
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalDittNavNaarPaalogginsnivaa4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

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
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til DITT_NAV: Bruker har logget på med nivaa4 de siste 18 mnd"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarIkkePaalogginsnivaa4OgIkkeSDPOgArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

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
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Bruker har ikke logget på med nivaa4 de siste 18 mnd"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanalPrintNaarPaalogginsnivaaIkkeFunnet4OgIkkeSDP() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

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
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Paaloggingsnivaa ikke tilgjengelig"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void shouldSetKanaPrintNaarPaalogginsnivaa4OgIkkeSDPOgIkkeArkivert() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
        capture = LogbackCapturingAppender.Factory.weaveInto(DokDistKanalService.LOG);

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
        assertEquals(DistribusjonKanalCode.PRINT, serviceResponse.getDistribusjonsKanal());
        assertThat(capture.getCapturedLogMessage(), is("BestemKanal: Sender melding til PRINT: Dokumentet er ikke arkivert"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    private DokDistKanalRequest.DokDistKanalRequestBuilder baseDokDistKanalRequestBuilder() {
        return DokDistKanalRequest.builder()
                .dokumentTypeId(DOKUMENTTYPEID)
                .mottakerId(FNR)
                .mottakerType(MottakerTypeCode.PERSON)
                .brukerId(FNR)
                .erArkivert(ER_ARKIVERT_TRUE)
                .tema("PEN");
    }

}

