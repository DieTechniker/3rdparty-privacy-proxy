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
	public void getClientIpV6AddressObfuscatedFullAddress() {
		when(mockRequest.getRemoteAddr()).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
		assertEquals(
			"no ip address found or could not obfuscate",
			"2001:db8:85a3::8a2e:0:0",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void getClientIpV6AddressObfuscatedSimplifiedAddress() {
		when(mockRequest.getRemoteAddr()).thenReturn("2b01:4b8:160:80e1::2");
		assertEquals(
			"no ip address found or could not obfuscate",
			"2b01:4b8:160:80e1::",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void getClientIpV6AddressObfuscatedLastSixSegmentsAreZero() {
		when(mockRequest.getRemoteAddr()).thenReturn("2001:db8::");
		assertEquals(
			"no ip address found or could not obfuscate",
			"2001:db8::",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void getClientIpV6AddressObfuscatedFirstSixSegmentsAreZero() {
		when(mockRequest.getRemoteAddr()).thenReturn("::1234:5678");
		assertEquals(
			"no ip address found or could not obfuscate",
			"::",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void getClientIpV6AddressObfuscatedMiddleFourSegmentsAreZero() {
		when(mockRequest.getRemoteAddr()).thenReturn("2001:db8::1234:5678");
		assertEquals(
			"no ip address found or could not obfuscate",
			"2001:db8::",
			RequestUtils.getClientIpAddress(mockRequest, true)
		);
	}

	@Test
	public void urlencode() {
		assertEquals(
			"url encoding failed",
			"abc%24%26%25%3F-%3A%3B",
			RequestUtils.urlencode("abc$&%?-:;")
		);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
