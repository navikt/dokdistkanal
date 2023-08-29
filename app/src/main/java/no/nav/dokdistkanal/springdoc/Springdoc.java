package no.nav.dokdistkanal.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Configuration
public class Springdoc {

	@Bean
	public OpenAPI dokdistkanalApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Dokdistkanal API")
						.description("""
								Her dokumenteres REST-grensesnittene til dokdistkanal. Til autentisering brukes OIDC-token (JWT via OAuth2.0).
								"""))
				.components(
						new Components()
								.addSecuritySchemes("Authorization",
										new SecurityScheme()
												.type(HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.in(HEADER)
												.description("Eksempel på verdi som skal inn i Value-feltet (Bearer trengs altså ikke å oppgis): 'eyAidH...'")
												.name(AUTHORIZATION)
								)
				)
				.addSecurityItem(
						new SecurityRequirement()
								.addList("Authorization")
				);
	}
}
