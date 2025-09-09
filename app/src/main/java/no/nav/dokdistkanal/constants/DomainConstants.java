package no.nav.dokdistkanal.constants;

import java.time.ZoneId;
import java.util.TimeZone;

public final class DomainConstants {

	public static final String APP_NAME = "dokdistkanal";
	public static final String NAV_ORGNUMMER = "0192:889640782";
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();
	public static final String HAL_JSON_VALUE = "application/hal+json";
	public static final int DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES = 45;
	public static final int DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE = 201;

	private DomainConstants() {
	}
}
