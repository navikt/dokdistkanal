package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
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

	@Test
	void skalFeilvalidereUgyldigMottarkerId() {
		var request = getBestemDistribusjonskanalRequest();
		request.setMottakerId(null);

		var violations = validator.validate(request);

		assertThat(violations)
				.hasSize(1)
				.allSatisfy(it -> {
					assertThat(it.getMessage()).isEqualTo("mottakerId må ha en verdi");
					assertThat(it.getPropertyPath().toString()).isEqualTo("mottakerId");
				});
	}

	@ParameterizedTest
	@MethodSource
	void skalFeilvalidereUgyldigUgyldigBrukerId(String brukerId, List<String> feilmeldinger) {
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

	static Stream<Arguments> skalFeilvalidereUgyldigUgyldigBrukerId() {
		return Stream.of(
				Arguments.of("", List.of("brukerId må ha en verdi")),
				Arguments.of(" ", List.of("brukerId må ha en verdi", "brukerId kan kun inneholde tall")),
				Arguments.of("123abc", List.of("brukerId kan kun inneholde tall")),
				Arguments.of("1234567891011", List.of("brukerId må være et tall med maks 11 siffer"))
		);
	}

	@Test
	void skalFeilvalidereUgyldigTema() {
		var request = getBestemDistribusjonskanalRequest();
		request.setTema(null);

		var violations = validator.validate(request);

		assertThat(violations)
				.hasSize(1)
				.allSatisfy(it -> {
					assertThat(it.getMessage()).isEqualTo("tema må ha en verdi");
					assertThat(it.getPropertyPath().toString()).isEqualTo("tema");
				});
	}

	private BestemDistribusjonskanalRequest getBestemDistribusjonskanalRequest() {
		return new BestemDistribusjonskanalRequest(
				"12345678910",
				"12345678910",
				"ABC",
				"DOK",
				false
		);
	}
}
