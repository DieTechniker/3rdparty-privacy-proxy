package de.tk.opensource.privacyproxy.util;

import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class ProxyHelperTest {

    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(8080));

    @Test
    void testNoExceptionsSelectProxy() throws MalformedURLException {
        ProxyHelper proxyHelper = new ProxyHelper(proxy, null, null, "");
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://www.domain.tld")));
    }

    @Test
    void testSelectProxy() throws MalformedURLException {
        ProxyHelper proxyHelper =
                new ProxyHelper(proxy, null, null, "domain.tld|*.wildcard.tld|any-tld.*|*.all.*");
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://some.external.url")));
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://www.domain.tld")));
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://otherdomain.tld")));
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://wildcard.tld")));
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://sub.any-tld.tld")));
        assertSame(proxy, proxyHelper.selectProxy(new URL("http://all")));
    }

    @Test
    void testSelectNoProxy() throws MalformedURLException {
        ProxyHelper proxyHelper =
                new ProxyHelper(proxy, null, null, "domain.tld|*.wildcard.tld|any-tld.*|*.all.*");
        assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://domain.tld")));
        assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://www.wildcard.tld")));
        assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://any-tld.tld")));
        assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://sub.all.tld")));
    }

    @Test
    void testDontMatchesEmptyPattern() {
        assertFalse(ProxyHelper.matches("", ""));
        assertFalse(ProxyHelper.matches("test", ""));
    }

    @Test
    void testMatchesWildcardAny() {
        assertTrue(ProxyHelper.matches("", "*"));
        assertTrue(ProxyHelper.matches("test", "*"));
    }

    @Test
    void testMatchesWildcardAfter() {
        assertTrue(ProxyHelper.matches("test", "test*"));
        assertTrue(ProxyHelper.matches("test", "tes*"));
        assertTrue(ProxyHelper.matches("test", "t*"));
        assertFalse(ProxyHelper.matches("test", "est*"));
    }

    @Test
    void testMatchesWildcardBefore() {
        assertTrue(ProxyHelper.matches("test", "*test"));
        assertTrue(ProxyHelper.matches("test", "*est"));
        assertTrue(ProxyHelper.matches("test", "*t"));
        assertFalse(ProxyHelper.matches("test", "*tes"));
    }

    @Test
    void testMatchesWildcardAtBeginAndEnd() {
        assertTrue(ProxyHelper.matches("test", "**"));
        assertTrue(ProxyHelper.matches("test", "*test*"));
        assertTrue(ProxyHelper.matches("test", "*t*"));
        assertTrue(ProxyHelper.matches("test", "*e*"));
        assertTrue(ProxyHelper.matches("test", "*s*"));
        assertTrue(ProxyHelper.matches("test", "*es*"));
        assertTrue(ProxyHelper.matches("test", "*tes*"));
        assertTrue(ProxyHelper.matches("test", "*est*"));
        assertTrue(ProxyHelper.matches("test", "*te*"));
        assertTrue(ProxyHelper.matches("test", "*st*"));
        assertFalse(ProxyHelper.matches("test", "*se*"));
    }

    @Test
    void testMatchesWithoutWildcard() {
        assertTrue(ProxyHelper.matches("test", "test"));
        assertFalse(ProxyHelper.matches("test", "est"));
        assertFalse(ProxyHelper.matches("test", "tes"));
        assertFalse(ProxyHelper.matches("test", "es"));
    }

    @Test
    void testMatchesCaseSensitive() {
        assertFalse(ProxyHelper.matches("test", "Test"));
        assertFalse(ProxyHelper.matches("Test", "test"));
        assertFalse(ProxyHelper.matches("test", "T*"));
        assertFalse(ProxyHelper.matches("Test", "t*"));
        assertFalse(ProxyHelper.matches("test", "*T"));
        assertFalse(ProxyHelper.matches("tesT", "*t"));
    }

    @Test
    void testContainingAsterisk() {
        assertFalse(ProxyHelper.matches("test", "t*t"));
        assertTrue(ProxyHelper.matches("t*t", "t*t"));
    }

    @Test
    void testRepeatingAsterisks() {
        assertFalse(ProxyHelper.matches("test", "t**"));
        assertFalse(ProxyHelper.matches("test", "**t"));
        assertFalse(ProxyHelper.matches("test", "***"));
        assertTrue(ProxyHelper.matches("t*t", "***"));
        assertTrue(ProxyHelper.matches("t*t", "t**"));
        assertTrue(ProxyHelper.matches("t*t", "**t"));
    }

    @Test
    void testProxyRoutePlannerInitialize() {
        ProxyHelper proxyHelper = new ProxyHelper(null, null, null, null);
        assertThat(proxyHelper.getProxyRoutePlanner(), is(instanceOf(SystemDefaultRoutePlanner.class)));

        proxyHelper = new ProxyHelper(null, "proxy.domain.de", 8080, null);
        assertThat(proxyHelper.getProxyRoutePlanner(), is(instanceOf(PrivacyProxyRoutePlanner.class)));
    }
}
