package no.nav.dokdistkanal.consumer.dki;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DigitalKontaktinformasjonConsumerTest {
    private final static String FNR = "12345678901";
    private final static String EPOSTADRESSE = "adresse@test.no";
    private final static String MOBIL = "123 45 678";
    private final static boolean RESERVASJON = true;
    private final static String LEVERANDORADRESSE = "Leverandøradresse";
    private final static String BRUKERADRESSE = "Brukeradresse";
    private final static boolean INKLUDER_SIKKER_DIGITAL_POST = true;

    private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);


    @Test
    public void shouldHentDKI() {
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class))).thenReturn(createResponse());

        DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST);

        assertThat(digitalKontaktinformasjonTo.getEpostadresse(), is(EPOSTADRESSE));
        assertThat(digitalKontaktinformasjonTo.getMobiltelefonnummer(), is(MOBIL));
        assertThat(digitalKontaktinformasjonTo.isReservasjon(), is(RESERVASJON));
        assertTrue(digitalKontaktinformasjonTo.isGyldigSertifikat());
        assertThat(digitalKontaktinformasjonTo.getBrukerAdresse(), is(BRUKERADRESSE));
        assertThat(digitalKontaktinformasjonTo.getLeverandoerAdresse(), is(LEVERANDORADRESSE));
    }

    @Test
    public void shouldReturnNullWhenRequestEmpty() {
        DigitalKontaktinformasjonTo response = createEmptyResponse();
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class))).thenReturn(response);

        DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST);

        assertNull(digitalKontaktinformasjonTo.getBrukerAdresse());
        assertNull(digitalKontaktinformasjonTo.getEpostadresse());
        assertNull(digitalKontaktinformasjonTo.getLeverandoerAdresse());
        assertNull(digitalKontaktinformasjonTo.getMobiltelefonnummer());
        assertFalse(digitalKontaktinformasjonTo.isReservasjon());
        assertFalse(digitalKontaktinformasjonTo.isGyldigSertifikat());
    }

    @Test
    public void shouldThrowFunctionalExceptionWhenSikkerhetsbegrensning() throws Exception {
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class)))
                .thenThrow(new DigitalKontaktinformasjonV2FunctionalException("Funksjonell feil"));

        assertThrows(DigitalKontaktinformasjonV2FunctionalException.class, () -> digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST));


    }

    @Test
    public void shouldReturnNullWhenNoKontaktinfo() {
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class)))
                .thenReturn(null);
        DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST);

        assertNull(digitalKontaktinformasjonTo);
    }

    @Test
    public void shouldThrowTechnicalExceptionWhenRuntimeException() throws Exception {
        when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class)))
                .thenThrow(new DigitalKontaktinformasjonV2TechnicalException("Teknisk feil"));
        assertThrows(DigitalKontaktinformasjonV2TechnicalException.class,
                () -> digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST));
    }

    private DigitalKontaktinformasjonTo createResponse() {
        return DigitalKontaktinformasjonTo.builder()
                .reservasjon(RESERVASJON)
                .mobiltelefonnummer(MOBIL)
                .leverandoerAdresse(LEVERANDORADRESSE)
                .epostadresse(EPOSTADRESSE)
                .gyldigSertifikat(true)
                .brukerAdresse(BRUKERADRESSE)
                .build();
    }

    private DigitalKontaktinformasjonTo createEmptyResponse() {
        return DigitalKontaktinformasjonTo.builder()
                .build();
    }
}
