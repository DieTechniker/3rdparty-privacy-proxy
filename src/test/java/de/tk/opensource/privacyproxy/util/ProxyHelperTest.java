/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;

public class ProxyHelperTest {

	Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(8080));

	@Test
	public void testNoExceptionsSelectProxy() throws MalformedURLException {
		ProxyHelper proxyHelper = new ProxyHelper(proxy, "");
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://www.domain.tld")));
	}

	@Test
	public void testSelectProxy() throws MalformedURLException {
		ProxyHelper proxyHelper =
			new ProxyHelper(proxy, "domain.tld|*.wildcard.tld|any-tld.*|*.all.*");
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://some.external.url")));
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://www.domain.tld")));
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://otherdomain.tld")));
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://wildcard.tld")));
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://sub.any-tld.tld")));
		assertSame(proxy, proxyHelper.selectProxy(new URL("http://all")));
	}

	@Test
	public void testSelectNoProxy() throws MalformedURLException {
		ProxyHelper proxyHelper =
			new ProxyHelper(proxy, "domain.tld|*.wildcard.tld|any-tld.*|*.all.*");
		assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://domain.tld")));
		assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://www.wildcard.tld")));
		assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://any-tld.tld")));
		assertSame(Proxy.NO_PROXY, proxyHelper.selectProxy(new URL("http://sub.all.tld")));
	}

	@Test
	public void testDontMatchesEmptyPattern() {
		assertFalse(ProxyHelper.matches("", ""));
		assertFalse(ProxyHelper.matches("test", ""));
	}

	@Test
	public void testMatchesWildcardAny() {
		assertTrue(ProxyHelper.matches("", "*"));
		assertTrue(ProxyHelper.matches("test", "*"));
	}

	@Test
	public void testMatchesWildcardAfter() {
		assertTrue(ProxyHelper.matches("test", "test*"));
		assertTrue(ProxyHelper.matches("test", "tes*"));
		assertTrue(ProxyHelper.matches("test", "t*"));
		assertFalse(ProxyHelper.matches("test", "est*"));
	}

	@Test
	public void testMatchesWildcardBefore() {
		assertTrue(ProxyHelper.matches("test", "*test"));
		assertTrue(ProxyHelper.matches("test", "*est"));
		assertTrue(ProxyHelper.matches("test", "*t"));
		assertFalse(ProxyHelper.matches("test", "*tes"));
	}

	@Test
	public void testMatchesWildcardAtBeginAndEnd() {
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
	public void testMatchesWithoutWildcard() {
		assertTrue(ProxyHelper.matches("test", "test"));
		assertFalse(ProxyHelper.matches("test", "est"));
		assertFalse(ProxyHelper.matches("test", "tes"));
		assertFalse(ProxyHelper.matches("test", "es"));
	}

	@Test
	public void testMatchesCaseSensitive() {
		assertFalse(ProxyHelper.matches("test", "Test"));
		assertFalse(ProxyHelper.matches("Test", "test"));
		assertFalse(ProxyHelper.matches("test", "T*"));
		assertFalse(ProxyHelper.matches("Test", "t*"));
		assertFalse(ProxyHelper.matches("test", "*T"));
		assertFalse(ProxyHelper.matches("tesT", "*t"));
	}

	@Test
	public void testContainingAsterisk() {
		assertFalse(ProxyHelper.matches("test", "t*t"));
		assertTrue(ProxyHelper.matches("t*t", "t*t"));
	}

	@Test
	public void testRepeatingAsterisks() {
		assertFalse(ProxyHelper.matches("test", "t**"));
		assertFalse(ProxyHelper.matches("test", "**t"));
		assertFalse(ProxyHelper.matches("test", "***"));
		assertTrue(ProxyHelper.matches("t*t", "***"));
		assertTrue(ProxyHelper.matches("t*t", "t**"));
		assertTrue(ProxyHelper.matches("t*t", "**t"));
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
