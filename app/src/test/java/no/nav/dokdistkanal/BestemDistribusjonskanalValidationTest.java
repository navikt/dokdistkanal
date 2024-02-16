package no.nav.dokdistkanal;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class BestemDistribusjonskanalValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void skalValidereGyldigBestemDistribusjonskanalRequest() {
		var request = getBestemDistribusjonskanalRequest();

		var violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@ParameterizedTest
	@MethodSource
	void skalFeilvalidereUgyldigMottakerId(String mottakerId, List<String> feilmeldinger) {
		var request = getBestemDistribusjonskanalRequest();
		request.setMottakerId(mottakerId);

		var violations = validator.validate(request);

		assertThat(violations)
				.hasSize(feilmeldinger.size())
				.allSatisfy(violation -> {
					assertThat(violation.getMessage()).isIn(feilmeldinger);
					assertThat(violation.getPropertyPath().toString()).isEqualTo("mottakerId");
				});
	}

	static Stream<Arguments> skalFeilvalidereUgyldigMottakerId() {
		return Stream.of(
				Arguments.of(null, List.of("mottakerId må ha en verdi")),
				Arguments.of("", List.of("mottakerId må ha en verdi")),
				Arguments.of("123456789012345678901", List.of("mottakerId kan ha maks 20 tegn"))
		);
	}

	@ParameterizedTest
	@MethodSource
	void skalFeilvalidereUgyldigBrukerId(String brukerId, List<String> feilmeldinger) {
		var request = getBestemDistribusjonskanalRequest();
		request.setBrukerId(brukerId);

		var violations = validator.validate(request);

		assertThat(violations)
				.hasSize(feilmeldinger.size())
				.allSatisfy(violation -> {
					assertThat(violation.getMessage()).isIn(feilmeldinger);
					assertThat(violation.getPropertyPath().toString()).isEqualTo("brukerId");
		});
	}

	static Stream<Arguments> skalFeilvalidereUgyldigBrukerId() {
		return Stream.of(
				Arguments.of(null, List.of("brukerId må ha en verdi")),
				Arguments.of("", List.of("brukerId må ha en verdi")),
				Arguments.of(" ", List.of("brukerId må ha en verdi", "brukerId kan kun inneholde tall")),
				Arguments.of("123abc", List.of("brukerId kan kun inneholde tall")),
				Arguments.of("1234567891011", List.of("brukerId kan ha maks 11 tegn"))
		);
	}

	@ParameterizedTest
	@MethodSource
	void skalFeilvalidereUgyldigTema(String tema, List<String> feilmeldinger) {
		var request = getBestemDistribusjonskanalRequest();
		request.setTema(tema);

		var violations = validator.validate(request);

		assertThat(violations)
				.hasSize(feilmeldinger.size())
				.allSatisfy(it -> {
					assertThat(it.getMessage()).isIn(feilmeldinger);
					assertThat(it.getPropertyPath().toString()).isEqualTo("tema");
				});
	}

	static Stream<Arguments> skalFeilvalidereUgyldigTema() {
		return Stream.of(
				Arguments.of(null, List.of("tema må ha en verdi")),
				Arguments.of("   ", List.of("tema må ha en verdi", "tema kan kun inneholde store bokstaver")),
				Arguments.of("far", List.of("tema kan kun inneholde store bokstaver")),
				Arguments.of("far1", List.of("tema må ha nøyaktig 3 tegn", "tema kan kun inneholde store bokstaver")),
				Arguments.of("FA", List.of("tema må ha nøyaktig 3 tegn")),
				Arguments.of("FARA", List.of("tema må ha nøyaktig 3 tegn"))
		);
	}

	private BestemDistribusjonskanalRequest getBestemDistribusjonskanalRequest() {
		return new BestemDistribusjonskanalRequest(
				"12345678910",
				"12345678910",
				"FAR",
				"DOK",
				false, 26
		);
	}
}
