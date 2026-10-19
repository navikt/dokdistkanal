package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Person;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Rolle;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Roller;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Type;
import no.nav.dokdistkanal.consumer.brreg.HentUnderenhetResponse;
import no.nav.dokdistkanal.consumer.brreg.HovedenhetResponse;
import no.nav.dokdistkanal.domain.Enhetsopplysninger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrregEnhetsregisterServiceTest {

	private static final String UNDERENHET_ORGNR = "123456789";
	private static final String HOVEDENHET_ORGNR = "987654321";

	private BrregEnhetsregisterConsumer brregEnhetsregisterConsumer;
	private BrregEnhetsregisterService brregEnhetsregisterService;

	@BeforeEach
	public void setup() {
		brregEnhetsregisterConsumer = mock(BrregEnhetsregisterConsumer.class);
		brregEnhetsregisterService = new BrregEnhetsregisterService(brregEnhetsregisterConsumer);
	}

	@Test
	void skalHenteHovedenhetNaarOrganisasjonIkkeErUnderenhet() {
		var hovedenhet = new HovedenhetResponse(HOVEDENHET_ORGNR, false, null);
		when(brregEnhetsregisterConsumer.hentHovedenhet(HOVEDENHET_ORGNR)).thenReturn(hovedenhet);

		var opplysninger = brregEnhetsregisterService.hentEnhetsopplysninger(HOVEDENHET_ORGNR);

		assertThat(opplysninger.underenhet()).isNull();
		assertThat(opplysninger.hovedenhet()).isEqualTo(new Enhetsopplysninger.Hovedenhet(HOVEDENHET_ORGNR, false, null));

		var rekkefolge = inOrder(brregEnhetsregisterConsumer);
		rekkefolge.verify(brregEnhetsregisterConsumer).hentUnderenhet(HOVEDENHET_ORGNR);
		rekkefolge.verify(brregEnhetsregisterConsumer).hentHovedenhet(HOVEDENHET_ORGNR);
		rekkefolge.verifyNoMoreInteractions();
	}

	@Test
	void skalHenteHovedenhetFraUnderenhetNaarOrganisasjonErUnderenhet() {
		var hovedenhet = new HovedenhetResponse(HOVEDENHET_ORGNR, false, null);
		when(brregEnhetsregisterConsumer.hentUnderenhet(UNDERENHET_ORGNR)).thenReturn(new HentUnderenhetResponse(UNDERENHET_ORGNR, HOVEDENHET_ORGNR, null));
		when(brregEnhetsregisterConsumer.hentHovedenhet(HOVEDENHET_ORGNR)).thenReturn(hovedenhet);

		var opplysninger = brregEnhetsregisterService.hentEnhetsopplysninger(UNDERENHET_ORGNR);

		assertThat(opplysninger.underenhet()).isEqualTo(new Enhetsopplysninger.Underenhet(UNDERENHET_ORGNR, HOVEDENHET_ORGNR, null));
		assertThat(opplysninger.hovedenhet()).isEqualTo(new Enhetsopplysninger.Hovedenhet(HOVEDENHET_ORGNR, false, null));

		var rekkefolge = inOrder(brregEnhetsregisterConsumer);
		rekkefolge.verify(brregEnhetsregisterConsumer).hentUnderenhet(UNDERENHET_ORGNR);
		rekkefolge.verify(brregEnhetsregisterConsumer).hentHovedenhet(HOVEDENHET_ORGNR);
		rekkefolge.verifyNoMoreInteractions();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = HOVEDENHET_ORGNR)
	void skalReturnereSlettetUnderenhetUtenHovedenhetsoppslag(String overordnetEnhet) {
		var slettedato = LocalDate.of(2026, 10, 1);
		when(brregEnhetsregisterConsumer.hentUnderenhet(UNDERENHET_ORGNR)).thenReturn(new HentUnderenhetResponse(UNDERENHET_ORGNR, overordnetEnhet, slettedato));

		var opplysninger = brregEnhetsregisterService.hentEnhetsopplysninger(UNDERENHET_ORGNR);

		assertThat(opplysninger.underenhet()).isEqualTo(new Enhetsopplysninger.Underenhet(UNDERENHET_ORGNR, overordnetEnhet, slettedato));
		assertThat(opplysninger.hovedenhet()).isNull();

		verify(brregEnhetsregisterConsumer, never()).hentHovedenhet(anyString());
	}

	@Test
	void skalReturnereTommeOpplysningerNaarOrganisasjonIkkeFinnes() {
		var opplysninger = brregEnhetsregisterService.hentEnhetsopplysninger(HOVEDENHET_ORGNR);

		assertThat(opplysninger.underenhet()).isNull();
		assertThat(opplysninger.hovedenhet()).isNull();

		verify(brregEnhetsregisterConsumer).hentUnderenhet(HOVEDENHET_ORGNR);
		verify(brregEnhetsregisterConsumer).hentHovedenhet(HOVEDENHET_ORGNR);
	}

	@ParameterizedTest
	@MethodSource
	public void skalReturnereGrupperolleType(String type, LocalDate fodselsdato, boolean erDoed, boolean harGyldigRolle) {
		when(brregEnhetsregisterConsumer.hentEnhetsRollegrupper(anyString())).thenReturn(createEnhetsRolleResponse(type, fodselsdato, erDoed));

		boolean harEnhetenGyldigRolle = brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(HOVEDENHET_ORGNR);

		assertThat(harEnhetenGyldigRolle).isEqualTo(harGyldigRolle);
	}

	private static Stream<Arguments> skalReturnereGrupperolleType() {
		return Stream.of(
				Arguments.of("DAGL", now().minusYears(30), false, true),
				Arguments.of("INNH", now().minusYears(30), false, true),
				Arguments.of("LEDE", now().minusYears(30), true, false),
				Arguments.of("BEST", null, false, false),
				Arguments.of("STYR", now().minusYears(30), false, false),
				Arguments.of("BOSS", now().minusYears(30), false, false)
		);
	}

	@ParameterizedTest
	@MethodSource
	public void skalFiltrereFratraadteRoller(String type, boolean fratraadt, boolean harGyldigRolle) {
		when(brregEnhetsregisterConsumer.hentEnhetsRollegrupper(anyString())).thenReturn(createEnhetsRolleResponse(type, now().minusYears(30), false, fratraadt));

		boolean harEnhetenGyldigRolle = brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(HOVEDENHET_ORGNR);

		assertThat(harEnhetenGyldigRolle).isEqualTo(harGyldigRolle);
	}

	private static Stream<Arguments> skalFiltrereFratraadteRoller() {
		return Stream.of(
				Arguments.of("DAGL", false, true),
				Arguments.of("DAGL", true, false),
				Arguments.of("LEDE", false, true),
				Arguments.of("LEDE", true, false)
		);
	}

	@Test
	public void skalReturnereFalseNaarAlleRollerMedGyldigTypeErFratraadt() {
		Person person = new Person(now().minusYears(30), false);

		Roller daglGruppe = new Roller(
				new Type("DAGL", "Daglig leder"),
				List.of(new Rolle(new Type("DAGL", "Daglig leder"), person, true))
		);
		Roller styrGruppe = new Roller(
				new Type("STYR", "Styre"),
				List.of(new Rolle(new Type("LEDE", "Styrets leder"), person, true))
		);

		EnhetsRolleResponse response = EnhetsRolleResponse.builder()
				.rollegrupper(List.of(daglGruppe, styrGruppe))
				.build();

		when(brregEnhetsregisterConsumer.hentEnhetsRollegrupper(anyString())).thenReturn(response);

		assertThat(brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(HOVEDENHET_ORGNR)).isFalse();
	}

	private EnhetsRolleResponse createEnhetsRolleResponse(String kode, LocalDate fodselsdato, boolean erDoed) {
		return createEnhetsRolleResponse(kode, fodselsdato, erDoed, false);
	}

	private EnhetsRolleResponse createEnhetsRolleResponse(String kode, LocalDate fodselsdato, boolean erDoed, boolean fratraadt) {
		Person person = new Person(fodselsdato, erDoed);
		Type type = new Type(kode, "");
		Rolle rolle = new Rolle(type, person, fratraadt);
		Roller roller = new Roller(null, List.of(rolle));

		return EnhetsRolleResponse.builder()
				.rollegrupper(List.of(roller))
				.build();
	}

}