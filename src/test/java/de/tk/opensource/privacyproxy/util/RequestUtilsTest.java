/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RequestUtilsTest {

	HttpServletRequest mockRequest = mock(HttpServletRequest.class);

	@Test
	public void getClientIpAddressFromRemoteAddr() {
		when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.104");
		assertEquals(
			"no ip address found",
			"101.102.103.104",
			RequestUtils.getClientIpAddress(mockRequest)
		);
	}

	@Test
	public void getClientIpAddressFromHeader() {
		when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.102");
		when(mockRequest.getHeader(anyString())).thenReturn("101.102.103.104");
		assertEquals(
			"no ip address found",
			"101.102.103.104",
			RequestUtils.getClientIpAddress(mockRequest)
		);
	}

	@Test
	public void getClientIpAddressObfuscated() {
		when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.104");
		assertEquals(
			"no ip address found or could not obfuscate",
			"101.102.0.0",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void getClientIpAddressObfuscatedShort() {
		when(mockRequest.getRemoteAddr()).thenReturn("1.2.3.4");
		assertEquals(
			"no ip address found or could not obfuscate",
			"1.2.0.0",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void getClientIpAddressObfuscatedInvalid() {
		when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.");
		assertEquals(
			"obfuscated though not a valid ip address was found",
			"101.102.103.",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void urlencode() {
		assertEquals(
			"url encoding failed",
			RequestUtils.urlencode("abc$&%?-:;"),
			"abc%24%26%25%3F-%3A%3B"
		);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
