/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.net.IPv6Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);
	private static final byte LENGTH_IPV4 = 4;
	private static final byte LENGTH_IPV6 = 8;
	private static final String IPV6_SHORT_FORM_SEPARATOR = "::";

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
		try {
			InetAddress address = InetAddress.getByName(ipAddress);
			if (address instanceof Inet6Address) {
				return obfuscateIpV6Address(ipAddress);

			}
			if (address instanceof Inet4Address) {
				return obfuscateIpV4Address(ipAddress);
			}
		} catch (UnknownHostException e) {
			LOGGER.debug(e.getMessage());
		}
		return ipAddress;
	}

	private static String obfuscateIpV4Address(String ipAddress) {
		String[] addr = ipAddress.split("\\.");
		if (addr.length == LENGTH_IPV4) {
			addr[LENGTH_IPV4 - 2] = addr[LENGTH_IPV4 - 1] = "0";
			return String.join(".", addr);
		}
		return ipAddress;
	}

	private static String obfuscateIpV6Address(String ipAddress) {
		String[] addr = ipAddress.split("\\:");
		int positionOfShortSeparator = ipAddress.indexOf(IPV6_SHORT_FORM_SEPARATOR);
		if (
			ipAddress.contains(IPV6_SHORT_FORM_SEPARATOR)
			&& positionOfShortSeparator != ipAddress.length() - IPV6_SHORT_FORM_SEPARATOR
				.length()
		) {
			addr =
				getFullIpV6Address(addr.length, positionOfShortSeparator, ipAddress).split("\\:");
		}

		if (addr.length == LENGTH_IPV6) {
			addr[LENGTH_IPV6 - 2] = addr[LENGTH_IPV6 - 1] = "0";
			return IPv6Utils.canonize(String.join(":", addr));
		}
		return ipAddress;
	}

	private static String getFullIpV6Address(
		int    amountOfIpv6Parts,
		int    positionOfShortSeparator,
		String ipAddress
	) {
		int startIndex = amountOfIpv6Parts;
		StringBuilder stringBuilder = new StringBuilder();
		if (positionOfShortSeparator != 0) {
			stringBuilder.append(":");
		} else {
			startIndex = startIndex - 1;
		}
		for (int i = startIndex; i <= LENGTH_IPV6; i++) {
			stringBuilder.append("0:");
		}
		return ipAddress.replace(IPV6_SHORT_FORM_SEPARATOR, stringBuilder.toString());
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
