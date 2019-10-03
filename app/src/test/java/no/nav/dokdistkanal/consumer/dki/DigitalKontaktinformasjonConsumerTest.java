package no.nav.dokdistkanal.consumer.dki;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DigitalKontaktinformasjonConsumerTest {
	private final static String FNR = "***gammelt_fnr***";
	private final static String EPOSTADRESSE = "adresse@test.no";
	private final static String MOBIL = "123 45 678";
	private final static boolean RESERVASJON = true;
	private final static String LEVERANDORADRESSE = "Leverandøradresse";
	private final static String BRUKERADRESSE = "Brukeradresse";
	private final static boolean INKLUDER_SIKKER_DIGITAL_POST = true;

	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldHentDKI() {
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class))).thenReturn(createResponse());

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST);

		assertThat(digitalKontaktinformasjonTo.getEpostadresse(), is(EPOSTADRESSE));
		assertThat(digitalKontaktinformasjonTo.getMobiltelefonnummer(), is(MOBIL));
		assertThat(digitalKontaktinformasjonTo.isReservasjon(), is(RESERVASJON));
		assertThat(digitalKontaktinformasjonTo.isSertifikat(), is(Boolean.TRUE));
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
		assertFalse(digitalKontaktinformasjonTo.isSertifikat());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenSikkerhetsbegrensning() throws Exception {
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(any(String.class), any(boolean.class)))
				.thenThrow(new DigitalKontaktinformasjonV2FunctionalException("Funksjonell feil"));
		expectedException.expect(DigitalKontaktinformasjonV2FunctionalException.class);

		digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST);
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
		expectedException.expect(DigitalKontaktinformasjonV2TechnicalException.class);

		digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(FNR, INKLUDER_SIKKER_DIGITAL_POST);
	}

	private DigitalKontaktinformasjonTo createResponse() {
		return DigitalKontaktinformasjonTo.builder()
				.reservasjon(RESERVASJON)
				.mobiltelefonnummer(MOBIL)
				.leverandoerAdresse(LEVERANDORADRESSE)
				.epostadresse(EPOSTADRESSE)
				.sertifikat(true)
				.brukerAdresse(BRUKERADRESSE)
				.build();
	}

	private DigitalKontaktinformasjonTo createEmptyResponse() {
		return DigitalKontaktinformasjonTo.builder()
				.build();
	}
}
