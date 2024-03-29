package de.tk.opensource.privacyproxy.util;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RequestUtilsTest {

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    @Test
    void getClientIpAddressFromRemoteAddr() {
        when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.104");
        assertEquals(
                "101.102.103.104",
                RequestUtils.getClientIpAddress(mockRequest),
                "no ip address found"
        );
    }

    @Test
    void getClientIpAddressFromHeader() {
        when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.102");
        when(mockRequest.getHeader(anyString())).thenReturn("101.102.103.104");
        assertEquals(
                "101.102.103.104",
                RequestUtils.getClientIpAddress(mockRequest),
                "no ip address found"
        );
    }

    @Test
    void getClientIpAddressObfuscated() {
        when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.104");
        assertEquals(
                "101.102.0.0",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void getClientIpAddressObfuscatedShort() {
        when(mockRequest.getRemoteAddr()).thenReturn("1.2.3.4");
        assertEquals(
                "1.2.0.0",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void getClientIpAddressObfuscatedInvalid() {
        when(mockRequest.getRemoteAddr()).thenReturn("101.102.103.");
        assertEquals(
                "101.102.103.",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "obfuscated though not a valid ip address was found"
        );
    }

    @Test
    void getClientIpV6AddressObfuscatedFullAddress() {
        when(mockRequest.getRemoteAddr()).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertEquals(
                "2001:db8:85a3::8a2e:0:0",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void getClientIpV6AddressObfuscatedSimplifiedAddress() {
        when(mockRequest.getRemoteAddr()).thenReturn("2b01:4b8:160:80e1::2");
        assertEquals(
                "2b01:4b8:160:80e1::",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void getClientIpV6AddressObfuscatedLastSixSegmentsAreZero() {
        when(mockRequest.getRemoteAddr()).thenReturn("2001:db8::");
        assertEquals(
                "2001:db8::",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void getClientIpV6AddressObfuscatedFirstSixSegmentsAreZero() {
        when(mockRequest.getRemoteAddr()).thenReturn("::1234:5678");
        assertEquals(
                "::",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void getClientIpV6AddressObfuscatedMiddleFourSegmentsAreZero() {
        when(mockRequest.getRemoteAddr()).thenReturn("2001:db8::1234:5678");
        assertEquals(
                "2001:db8::",
                RequestUtils.getClientIpAddress(mockRequest, true),
                "no ip address found or could not obfuscate"
        );
    }

    @Test
    void urlencode() {
        assertEquals(
                "abc%24%26%25%3F-%3A%3B",
                RequestUtils.urlencode("abc$&%?-:;"),
                "url encoding failed"
        );
    }
}
