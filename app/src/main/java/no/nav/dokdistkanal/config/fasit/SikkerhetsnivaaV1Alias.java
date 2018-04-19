package no.nav.dokdistkanal.config.fasit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.bval.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

/**
 * @author Ketill Fenne, Visma Consulting
 */
@Getter
@Setter
@ToString
@ConfigurationProperties("SIKKERHETSNIVAA_V1")
@Validated
public class SikkerhetsnivaaV1Alias {
	@NotEmpty
	private String url;
	private String description;
	@Min(1)
	private int readtimeoutms;
	@Min(1)
	private int connecttimeoutms;
}
