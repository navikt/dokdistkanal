package no.nav.dokdistkanal.config.springdoc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Operation(summary = "Bestemmer distribusjonskanal for et dokument basert på informasjon om mottaker, bruker, tema dokumenttype og arkivering")
@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "OK - Bestemt kanal returneres sammen med hvilken regel kanalen ble bestemt på bakgrunn av og begrunnelse for valget av denne.",
				content = @Content(
						mediaType = "application/json",
						schema = @Schema(implementation = BestemDistribusjonskanalResponse.class)
				)
		),
		@ApiResponse(responseCode = "401",
				description = "Ugyldig OIDC token. Denne feilen gis dersom tokenet ikke har riktig format eller er utgått.",
				content = @Content
		),
		@ApiResponse(
				responseCode = "400",
				description = "Bad Request - Input feilet validering. Eksempler: mottakerId er ikke satt, brukerId er ikke et tall eller består av flere enn 11 siffer.",
				content = @Content
		),
		@ApiResponse(
				responseCode = "500",
				description = "Internal Server Error - Inter teknisk feil. Eksempel: Kall mot en annen tjeneste feiler."
		)
})
public @interface SwaggerBestemDistribusjonskanal {
}
