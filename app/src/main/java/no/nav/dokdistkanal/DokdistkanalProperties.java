package no.nav.dokdistkanal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("dokdistkanal")
@Validated
public class DokdistkanalProperties {

	private final Proxy proxy = new Proxy();


	@Data
	@Validated
	public static class Proxy {
		private String host;
		private int port;

		public boolean isSet() {
			return (host!=null && !host.equals(""));
		}
	}
}
