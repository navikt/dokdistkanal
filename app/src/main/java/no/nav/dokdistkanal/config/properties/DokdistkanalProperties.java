package no.nav.dokdistkanal.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("dokdistkanal")
@Validated
public class DokdistkanalProperties {

	private final Altinn altinn = new Altinn();
	private final Endpoints endpoints = new Endpoints();
	private final Endpoint serviceRegistry = new Endpoint();
	private final EnhetsregisterEndpoint enhetsregister = new EnhetsregisterEndpoint();

	@Data
	public static class Endpoints {
		@NotNull private AzureEndpoint digdirKrrProxy;
		@NotNull private AzureEndpoint pdl;
		@NotNull private Endpoint dokmet;
	}

	@Data
	public static class AzureEndpoint {
		@NotEmpty
		private String url;

		@NotEmpty
		private String scope;
	}

	@Data
	@Validated
	public static final class Altinn {
		@NotEmpty
		private String url;
		@NotEmpty
		private String apiKey;
	}

	@Data
	@Validated
	public static final class EnhetsregisterEndpoint {
		@NotEmpty
		private String url;
	}

	@Data
	@Validated
	public static class Endpoint {
		@NotEmpty
		private String url;
	}

}
