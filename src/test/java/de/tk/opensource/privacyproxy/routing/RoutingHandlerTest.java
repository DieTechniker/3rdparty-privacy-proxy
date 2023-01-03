package de.tk.opensource.privacyproxy.routing;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutingHandlerTest {

    @Test
    void testQueryString() {
        Map<String, String> parameter = new LinkedHashMap<>();
        parameter.put("one", "parameter1");
        parameter.put("two", "some, special chars?");

        RoutingHandler handler = new RoutingHandler() {
        };

        assertEquals(
                "one=parameter1&two=some%2C+special+chars%3F",
                handler.filterQueryString(parameter)
        );
    }

    @Test
    void testQueryStringWithoutEncoding() {
        Map<String, String> parameter = new LinkedHashMap<>();
        parameter.put("param", "value, test");

        RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String transformQueryParam(String name, String value) {
                        return value;
                    }
                };

        assertEquals("param=value, test", handler.filterQueryString(parameter));
    }

    @Test
    void testQueryStringFiltered() {
        Map<String, String> parameter = new LinkedHashMap<>();
        parameter.put("restricted", "hidden value");
        parameter.put("param", "some value");

        RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String[] getBlacklistedQueryParams() {
                        return new String[]{"restricted"};
                    }
                };

        assertEquals("param=some+value", handler.filterQueryString(parameter));
    }

    @Test
    void testWhitelistResponseHeaders() {
        final RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String[] getWhitelistedResponseHeaders() {
                        return new String[]{"Set-Cookie"};
                    }
                };

        final HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.add("Set-Cookie", "schnippen dippen");
        mockHeaders.add("cache-control", "max-age=0, private, must-revalidate");
        mockHeaders.add("referrer-policy", "strict-origin-when-cross-origin");
        mockHeaders.add("Content-Type", "text/plain");

        final HttpHeaders whitelistedHeaders = handler.whitelistResponseHeaders(mockHeaders);
        assertThat(whitelistedHeaders.entrySet(), hasSize(3));
        assertThat(
                whitelistedHeaders.keySet(),
                contains("Cache-Control", "Content-Type", "Set-Cookie")
        );
        assertThat(whitelistedHeaders.get("Cache-Control"), contains("no-cache"));
        assertThat(whitelistedHeaders.get("Content-Type"), contains("text/plain"));
        assertThat(whitelistedHeaders.get("Set-Cookie"), contains("schnippen dippen"));
    }

    @Test
    void testGetRequestHeaders() {
        final RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String[] getWhitelistedRequestHeaders() {
                        return new String[]{"user-agent", "referrer"};
                    }

                    @Override
                    protected Map<String, String> getAdditionalRequestHeaders(
                            HttpServletRequest request
                    ) {
                        return Collections.singletonMap("Cookie", "key=schnippen dippen");
                    }
                };

        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36"
        );
        request.addHeader("referrer", "https://www.tk.de");
        request.addHeader("accept-language", "en,de-DE;q=0.9,de;q=0.8,en-US;q=0.7");

        final HttpHeaders whitelistedRequestHeaders = handler.getRequestHeaders(request);
        assertThat(whitelistedRequestHeaders.entrySet(), hasSize(3));
        assertThat(
                whitelistedRequestHeaders.keySet(),
                contains("user-agent", "referrer", "Cookie")
        );
        assertThat(
                whitelistedRequestHeaders.get("user-agent"),
                contains(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36"
                )
        );
        assertThat(whitelistedRequestHeaders.get("referrer"), contains("https://www.tk.de"));
        assertThat(whitelistedRequestHeaders.get("Cookie"), contains("key=schnippen dippen"));
    }

    @Test
    void testAddWhitelistedCookiesByPrefix() {
        final RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String[] getWhitelistedCookieNames() {
                        return new String[]{"wt_", "wteid_"};
                    }

                    @Override
                    protected CookieNameMatchType getCookieNameMatchType() {
                        return CookieNameMatchType.PREFIX;
                    }
                };

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final Cookie cookie1 = new Cookie("wt_rla", "1337");
        final Cookie cookie2 = new Cookie("wt_ttv2_7331", "0815");
        final Cookie cookie3 = new Cookie("wteid_111", "111");
        request.setCookies(cookie1, cookie2, cookie3);

        final HttpHeaders headers = new HttpHeaders();
        handler.addWhitelistedCookies(request, headers);
        assertThat(headers.entrySet(), hasSize(1));
        assertThat(headers, hasKey("Cookie"));
        assertThat(
                headers.get("Cookie"),
                contains("wt_rla=1337; path=/; wt_ttv2_7331=0815; path=/; wteid_111=111; path=/")
        );
    }

    @Test
    void testAddWhitelistedCookies() {
        final RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String[] getWhitelistedCookieNames() {
                        return new String[]{"wt_rla", "wteid_"};
                    }

                    @Override
                    protected CookieNameMatchType getCookieNameMatchType() {
                        return CookieNameMatchType.FULL;
                    }
                };

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final Cookie cookie1 = new Cookie("wt_rla", "1337");
        final Cookie cookie2 = new Cookie("wt_ttv2_7331", "0815");
        final Cookie cookie3 = new Cookie("wteid_111", "111");
        request.setCookies(cookie1, cookie2, cookie3);

        final HttpHeaders headers = new HttpHeaders();
        handler.addWhitelistedCookies(request, headers);
        assertThat(headers.entrySet(), hasSize(1));
        assertThat(headers, hasKey("Cookie"));
        assertThat(headers.get("Cookie"), contains("wt_rla=1337; path=/"));
    }

    @Test
    void testFilterRequestBody() {
        final Map<String, String> requestBody = new HashMap<>();
        requestBody.put("restricted", "hidden value");
        requestBody.put("param", "some value");

        RoutingHandler handler =
                new RoutingHandler() {
                    @Override
                    protected String[] getBlacklistedQueryParams() {
                        return new String[]{"restricted"};
                    }
                };

        handler.filterRequestBody(requestBody);
        assertThat(requestBody.size(), is(1));
        assertThat(requestBody, hasKey("param"));
    }

    @Test()
    void testFilterRequestBodyNoException() {
        assertDoesNotThrow(() -> new RoutingHandler() {
        }.filterRequestBody(null));
    }
}
