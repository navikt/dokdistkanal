package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Person;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Rolle;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Roller;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse.Type;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrregEnhetsregisterServiceTest {

	private static final String MOTTAKER_ID = "123456789";

	private BrregEnhetsregisterConsumer brregEnhetsregisterConsumer;

	private BrregEnhetsregisterService brregEnhetsregisterService;

	@BeforeEach
	public void setup() {
		brregEnhetsregisterConsumer = mock(BrregEnhetsregisterConsumer.class);
		brregEnhetsregisterService = new BrregEnhetsregisterService(brregEnhetsregisterConsumer);
	}

	@ParameterizedTest
	@MethodSource
	public void skalReturnereGrupperolleType(String type, LocalDate fodselsdato, boolean erDoed, boolean harGyldigRolle) {
		when(brregEnhetsregisterConsumer.hentEnhetsRollegrupper(anyString())).thenReturn(createEnhetsRolleResponse(type, fodselsdato, erDoed));

		boolean harEnhetenGyldigRolle = brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(MOTTAKER_ID);

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

		boolean harEnhetenGyldigRolle = brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(MOTTAKER_ID);

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

		assertThat(brregEnhetsregisterService.harEnhetenGyldigRolletypeForDpvt(MOTTAKER_ID)).isFalse();
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