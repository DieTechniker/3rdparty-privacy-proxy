/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

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
		assertEquals(false, ProxyHelper.matches("", ""));
		assertEquals(false, ProxyHelper.matches("test", ""));
	}

	@Test
	public void testMatchesWildcardAny() {
		assertEquals(true, ProxyHelper.matches("", "*"));
		assertEquals(true, ProxyHelper.matches("test", "*"));
	}

	@Test
	public void testMatchesWildcardAfter() {
		assertEquals(true, ProxyHelper.matches("test", "test*"));
		assertEquals(true, ProxyHelper.matches("test", "tes*"));
		assertEquals(true, ProxyHelper.matches("test", "t*"));
		assertEquals(false, ProxyHelper.matches("test", "est*"));
	}

	@Test
	public void testMatchesWildcardBefore() {
		assertEquals(true, ProxyHelper.matches("test", "*test"));
		assertEquals(true, ProxyHelper.matches("test", "*est"));
		assertEquals(true, ProxyHelper.matches("test", "*t"));
		assertEquals(false, ProxyHelper.matches("test", "*tes"));
	}

	@Test
	public void testMatchesWildcardAtBeginAndEnd() {
		assertEquals(true, ProxyHelper.matches("test", "**"));
		assertEquals(true, ProxyHelper.matches("test", "*test*"));
		assertEquals(true, ProxyHelper.matches("test", "*t*"));
		assertEquals(true, ProxyHelper.matches("test", "*e*"));
		assertEquals(true, ProxyHelper.matches("test", "*s*"));
		assertEquals(true, ProxyHelper.matches("test", "*es*"));
		assertEquals(true, ProxyHelper.matches("test", "*tes*"));
		assertEquals(true, ProxyHelper.matches("test", "*est*"));
		assertEquals(true, ProxyHelper.matches("test", "*te*"));
		assertEquals(true, ProxyHelper.matches("test", "*st*"));
		assertEquals(false, ProxyHelper.matches("test", "*se*"));
	}

	@Test
	public void testMatchesWithoutWildcard() {
		assertEquals(true, ProxyHelper.matches("test", "test"));
		assertEquals(false, ProxyHelper.matches("test", "est"));
		assertEquals(false, ProxyHelper.matches("test", "tes"));
		assertEquals(false, ProxyHelper.matches("test", "es"));
	}

	@Test
	public void testMatchesCaseSensitive() {
		assertEquals(false, ProxyHelper.matches("test", "Test"));
		assertEquals(false, ProxyHelper.matches("Test", "test"));
		assertEquals(false, ProxyHelper.matches("test", "T*"));
		assertEquals(false, ProxyHelper.matches("Test", "t*"));
		assertEquals(false, ProxyHelper.matches("test", "*T"));
		assertEquals(false, ProxyHelper.matches("tesT", "*t"));
	}

	@Test
	public void testContainingAsterisk() {
		assertEquals(false, ProxyHelper.matches("test", "t*t"));
		assertEquals(true, ProxyHelper.matches("t*t", "t*t"));
	}

	@Test
	public void testRepeatingAsterisks() {
		assertEquals(false, ProxyHelper.matches("test", "t**"));
		assertEquals(false, ProxyHelper.matches("test", "**t"));
		assertEquals(false, ProxyHelper.matches("test", "***"));
		assertEquals(true, ProxyHelper.matches("t*t", "***"));
		assertEquals(true, ProxyHelper.matches("t*t", "t**"));
		assertEquals(true, ProxyHelper.matches("t*t", "**t"));
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
