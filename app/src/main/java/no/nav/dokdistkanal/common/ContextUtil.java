package no.nav.dokdistkanal.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ContextUtil {

	private ContextUtil() {}

	public static String getConsumerId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || ("anonymousUser").equalsIgnoreCase(authentication.getName())) {
			return "Ukjent";
		}
		return authentication.getName();
	}

}
