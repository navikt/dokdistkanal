package no.nav.dokdistkanal.constants;

import java.time.ZoneId;
import java.util.TimeZone;

public final class DomainConstants {

	public static final String APP_NAME = "dokdistkanal";
	public static final String NAV_ORGNUMMER = "0192:889640782";
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

	public static final int DELAY_SHORT = 500;
	public static final int MULTIPLIER_SHORT = 2;

	private DomainConstants() {
	}

}
