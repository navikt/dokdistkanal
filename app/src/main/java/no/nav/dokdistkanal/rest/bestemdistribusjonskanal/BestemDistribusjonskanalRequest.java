package no.nav.dokdistkanal.rest.bestemdistribusjonskanal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BestemDistribusjonskanalRequest {
	@Schema(description = "Identifikator for mottakeren av dokumentet")
	@NotBlank(message = "mottakerId må ha en verdi")
	String mottakerId;

	@Schema(description = "Identifikator for brukeren som er sakspart")
	@NotBlank(message = "brukerId må ha en verdi")
	@Pattern(regexp = "^[0-9]*$", message = "brukerId kan kun inneholde tall")
	@Size(max = 11, message = "brukerId må være et tall med maks 11 siffer")
	String brukerId;

	@Schema(description = "Temaet som forsendelsen tilhører, for eksempel \"FOR\" (foreldrepenger)")
	@NotBlank(message = "tema må ha en verdi")
	@Size(max = 3, min = 3, message = "tema må bestå av nøyaktig 3 tegn")
	String tema;

	@Schema(description = "Typen til dokumentet som skal distribueres")
	String dokumenttypeId;

	@Schema(description = "Om dokumentet er arkivert i Joark eller ikke")
	boolean erArkivert;
}
