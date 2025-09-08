package no.nav.dokdistkanal.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("dokdistkanal")
@Validated
public class DokdistkanalProperties {

	@Valid
	private final Altinn altinn = new Altinn();
	@Valid
	private final Endpoints endpoints = new Endpoints();
	@Valid
	private final Endpoint serviceRegistry = new Endpoint();
	@Valid
	private final Dpo dpo = new Dpo();
	@Valid
	private final EnhetsregisterEndpoint enhetsregister = new EnhetsregisterEndpoint();

	@Data
	public static class Endpoints {
		@Valid
		private AzureEndpoint digdirKrrProxy;
		@Valid
		private AzureEndpoint pdl;
		@Valid
		private Endpoint dokmet;
	}

	@Data
	public static class AzureEndpoint {
		@NotEmpty
		private String url;

		@NotEmpty
		private String scope;
	}

	@Data
	public static final class Altinn {
		@NotEmpty
		private String url;
		@NotEmpty
		private String apiKey;
	}

	@Data
	public static final class EnhetsregisterEndpoint {
		@NotEmpty
		private String url;
	}

	@Data
	public static final class Dpo {
		@NotEmpty
		private String clientId;
		@NotEmpty
		private String scope;

	}

	@Data
	public static class Endpoint {
		@NotEmpty
		private String url;
	}

}
