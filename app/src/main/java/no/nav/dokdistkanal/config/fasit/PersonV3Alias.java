package no.nav.dokdistkanal.config.fasit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import javax.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Getter
@Setter
@ToString
@ConfigurationProperties("virksomhet-person-v3")
@Validated
public class PersonV3Alias {
	@NotEmpty
	private String endpointurl;
	private String description;
	@Min(1)
	private int readtimeoutms;
	@Min(1)
	private int connecttimeoutms;
}