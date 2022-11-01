package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.exceptions.functional.CouldNotDecodeBasicAuthToken;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistkanal.constants.MDCConstants.CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.USER_ID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.MDC.put;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class PopulateMDCHandler implements HandlerInterceptor {

	private static final String CHARSET = UTF_8.name();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		populateConsumerId(request);
		populateUserId(request);
		return true;
	}

	private void populateConsumerId(HttpServletRequest request) {
		String navConsumerId = isNotBlank(request.getHeader(NAV_CONSUMER_ID)) ? request.getHeader(NAV_CONSUMER_ID) : request.getHeader(CONSUMER_ID);
		String username = getUsernameFromBasicAuth(request);
		if (isNotBlank(navConsumerId)) {
			put(CONSUMER_ID, navConsumerId);
		}

		if (isNotBlank(username)) {
			put(CONSUMER_ID, username);
		}
	}

	private void populateUserId(HttpServletRequest request) {
		final String navUserId = request.getHeader(USER_ID);
		if (isNotBlank(navUserId)) {
			put(USER_ID, navUserId);
		}
	}

	private String getUsernameFromBasicAuth(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(AUTHORIZATION);
		if (isNotBlank(authorizationHeader) && authorizationHeader.startsWith("Basic")) {
			try {
				String[] strings = decodeBasicAuth(authorizationHeader);
				return strings[0];
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public static String[] decodeBasicAuth(String header) {
		byte[] decoded;
		try {
			byte[] base64Token = header.substring(6).getBytes(CHARSET);
			decoded = Base64.getDecoder().decode(base64Token);
			String token = new String(decoded, CHARSET);
			int delim = token.indexOf(':');
			if (delim == -1) {
				throw new CouldNotDecodeBasicAuthToken("Decode av basicAuthToken feilet");
			}
			return new String[]{token.substring(0, delim), token.substring(delim + 1)};
		} catch (IllegalArgumentException | UnsupportedEncodingException e) {
			throw new CouldNotDecodeBasicAuthToken("Decode av basicAuthToken feilet");
		}
	}
}
