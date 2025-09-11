package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BestemDistribusjonskanalRequest {

	@Schema(description = "Identifikator for mottaker av dokumentet")
	@NotBlank(message = "mottakerId må ha en verdi")
	@Size(max = 20, message = "mottakerId kan ha maks 20 tegn")
	String mottakerId;

	@Schema(description = "Identifikator for bruker/organisasjon")
	@NotBlank(message = "brukerId må ha en verdi")
	@Pattern(regexp = "^[0-9]*$", message = "brukerId kan kun inneholde tall")
	@Size(max = 11, message = "brukerId kan ha maks 11 tegn")
	String brukerId;

	@Schema(description = "Tema som forsendelsen tilhører.", example = "FOR")
	@NotBlank(message = "tema må ha en verdi")
	@Pattern(regexp = "^[A-Z]*$", message = "tema kan kun inneholde store bokstaver")
	@Size(max = 3, min = 3, message = "tema må ha nøyaktig 3 tegn")
	String tema;

	@Schema(description = "Typen dokument som skal distribueres", defaultValue = "U000001")
	String dokumenttypeId;

	@Schema(description = "Om dokumentet er arkivert i Joark eller ikke")
	boolean erArkivert;

	@PositiveOrZero
	@Schema(description = "Dokumentenes samlede filstørrelse i megabytes", example = "20", defaultValue = "0")
	Integer forsendelseStoerrelse;

	@PositiveOrZero
	@Schema(description = "Antall dokumenter i forsendelsen totalt", example = "99", defaultValue = "0")
	Integer antallDokumenter;

	@Schema(description = "Type metadata som følger forsendelsen", example = "DPO_AVTALEMELDING")
	String forsendelseMetadataType;
}
