package no.nav.dokdistkanal.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("dokdistkanal")
@Validated
public class DokdistkanalProperties {

	private final Proxy proxy = new Proxy();
	private final Serviceuser serviceuser = new Serviceuser();
	private final Sikkerhetsnivaa sikkerhetsnivaa = new Sikkerhetsnivaa();
	private final Altinn altinn = new Altinn();

	@Data
	@Validated
	public static class Serviceuser {
		@NotEmpty
		private String username;
		@NotEmpty
		private String password;
	}

	@Data
	@Validated
	public static class Sikkerhetsnivaa {
		@NotEmpty
		private String url;
		private String description;
		@Min(1)
		private int readtimeoutms;
		@Min(1)
		private int connecttimeoutms;
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
	public static class Proxy {
		private String host;
		private int port;

		public boolean isSet() {
			return (host != null && !host.equals(""));
		}
	}
}
