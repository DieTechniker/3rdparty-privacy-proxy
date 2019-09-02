/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);

	protected static final String[] IP_HEADER_CANDIDATES =
		{
			"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR",
			"HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
			"HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
		};

	private RequestUtils() {
	}

	/**
	 * Lookup IP address in request headers or use {@link HttpServletRequest#getRemoteAddr()} as
	 * fallback.
	 *
	 * @param   request
	 *
	 * @return  ip address from request header, use {@link HttpServletRequest#getRemoteAddr()} as
	 *          fallback
	 *
	 * @see     #IP_HEADER_CANDIDATES
	 */
	public static String getClientIpAddress(HttpServletRequest request) {
		return getClientIpAddress(request, false);
	}

	/**
	 * Lookup IP address in request headers or use {@link HttpServletRequest#getRemoteAddr()} as
	 * fallback and obfuscate IP address.
	 *
	 * @param   request
	 * @param   obfuscate  true if last two bytes of IP address should be set to zero
	 *
	 * @return  ip address from request header, use {@link HttpServletRequest#getRemoteAddr()} as
	 *          fallback
	 *
	 * @see     #IP_HEADER_CANDIDATES
	 */
	public static String getClientIpAddress(HttpServletRequest request, boolean obfuscate) {
		String ipAddress = request.getRemoteAddr(); // fallback
		for (String header : IP_HEADER_CANDIDATES) {
			String ipList = request.getHeader(header);
			if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
				ipAddress = ipList.split(",")[0];
				break;
			}
		}

		if (obfuscate) {
			ipAddress = obfuscateIpAddress(ipAddress);
		}

		return ipAddress;
	}

	/**
	 * Obfuscates last two bytes of an IP address (set to zero).
	 *
	 * @param   ipAddress
	 *
	 * @return  obfuscated IP address if a valid one was found, input value otherwise
	 */
	public static String obfuscateIpAddress(String ipAddress) {
		String[] addr = ipAddress.split("\\.");
		if (addr.length == 4) {
			addr[2] = addr[3] = "0";
			return String.join(".", addr);
		}
		return ipAddress; // fallback, return input value
	}

	/**
	 * @return  url encoded value
	 */
	public static String urlencode(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Url encoding failed", e);
			return "";
		}
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
