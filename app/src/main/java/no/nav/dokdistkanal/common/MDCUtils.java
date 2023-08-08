package no.nav.dokdistkanal.common;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class MDCUtils {

	private MDCUtils() {
	}

	public static String getOrCreateCallId(final String navCallid) {
		return getOrCreateCallId(navCallid, null);
	}

	public static String getOrCreateCallId(final String navCallid, final String dokCallId) {
		if (isNotBlank(navCallid)) {
			return navCallid;
		}
		if (isNotBlank(dokCallId)) {
			return dokCallId;
		}
		return UUID.randomUUID().toString();
	}

}
