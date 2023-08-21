package no.nav.dokdistkanal.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class Springdoc {

	@Bean
	public OpenAPI dokdistkanalApi(@Value("${NAIS_APP_IMAGE:1-SNAPSHOT}") String version) {
		return new OpenAPI()
				.info(new Info()
						.title("Dokdistkanal API")
						.description("""
								Her dokumenteres REST-grensesnittene til dokdistkanal. Til autentisering brukes OIDC-token (JWT via OAuth2.0).
								""")
						.version(version))
				.components(
						new Components()
								.addSecuritySchemes("Authorization",
										new SecurityScheme()
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.in(SecurityScheme.In.HEADER)
												.description("Eksempel på verdi som skal inn i Value-feltet (Bearer trengs altså ikke å oppgis): 'eyAidH...'")
												.name(HttpHeaders.AUTHORIZATION)
								)
				)
				.addSecurityItem(
						new SecurityRequirement()
								.addList("Authorization")
				);
	}
}
