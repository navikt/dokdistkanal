package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.time.LocalDate.now;
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

		Assertions.assertThat(harEnhetenGyldigRolle).isEqualTo(harGyldigRolle);
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

	private EnhetsRolleResponse createEnhetsRolleResponse(String kode, LocalDate fodselsdato, boolean erDoed) {
		EnhetsRolleResponse.Person person = new EnhetsRolleResponse.Person(fodselsdato, erDoed);
		EnhetsRolleResponse.Type type = new EnhetsRolleResponse.Type(kode, "");
		EnhetsRolleResponse.Rolle rolle = new EnhetsRolleResponse.Rolle(type, person);
		EnhetsRolleResponse.Roller roller = new EnhetsRolleResponse.Roller(null, List.of(rolle));

		return EnhetsRolleResponse.builder()
				.rollegrupper(List.of(roller))
				.build();
	}

}