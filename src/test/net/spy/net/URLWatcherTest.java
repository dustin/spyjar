// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 29F051AE-1110-11D9-A274-000A957659CC

package net.spy.net;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test the URLWatcher thing.
 */
public class URLWatcherTest extends TestCase {

	private URLWatcher uw=null;

	/** 
	 * Get the URLWatcher.
	 */
	@Override
	protected void setUp() {
		uw=new TestURLWatcher();
	}

	/** 
	 * Get rid of the URLWatcher.
	 */
	@Override
	protected void tearDown() {
		uw.shutdown();
		uw=null;
	}

	/**
	 * Test singleton stuff.
	 */
	public void testSingleton() throws Exception {
		URLWatcher.setInstance(uw);
		assertSame(uw, URLWatcher.getInstance());
		URLWatcher.setInstance(null);
	}

	public void testStringing() throws Exception {
		assertTrue(uw.toString().endsWith("0 pages monitored"));
	}

	/** 
	 * Test basic URL watching functionality.
	 */
	public void testBasicURLWatching()
		throws IOException, InterruptedException {

		URL u=new URL("http://bleu.west.spy.net/~dustin/util/getdate.jsp");
		String c1=uw.getContent(u);
		assertNotNull("Did not get content from " + u, c1);
		Thread.sleep(100);
		String c2=uw.getContent(u);
		assertNotNull(c2);

		assertSame("Different results", c1, c2);
		assertEquals(c1 + "!=" + c2, c1, c2);
	}

	/** 
	 * Test a URLWatcher with a manual URL setting.
	 */
	public void testManualURLWatching()
		throws IOException, InterruptedException {

		URL u=new URL("http://bleu.west.spy.net/~dustin/util/getdate.jsp");

		if(uw.isWatching(u)) {
			System.err.println(uw.getContent(u));
		}

		assertTrue("Shouldn't be watching that URL yet.", (!uw.isWatching(u)));

		URLItem ui=new TestURLItem(u, 300);
		uw.startWatching(ui);

		String s1=uw.getContent(u);
		assertNotNull("Initial content not returned", s1);
		String s2=uw.getContent(u);
		assertNotNull("Second content not returned", s1);

		assertSame("Different results second time", s1, s2);

		Thread.sleep(1500);

		String s3=uw.getContent(u);
		assertNotNull("Third content not returned", s3);
		assertTrue("Expected different results on third run", (!s2.equals(s3)));
	}

	/** 
	 * Test with several URLs.
	 */
	public void testLotsOfURLs() throws IOException, InterruptedException {
		String[] urls={"http://bleu.west.spy.net/",
			"http://bleu.west.spy.net/~dustin/",
			"http://bleu.west.spy.net/~dustin/music/",
			"http://bleu.west.spy.net/~dustin/eiffel/",
			"http://bleu.west.spy.net/~dustin/projects/",
			"http://bleu.west.spy.net/~dustin/wa/bleu/",
			"http://bleu.west.spy.net/~dustin/wa/prop/",
			"http://bleu.west.spy.net/~dustin/projects/filemonitor.xtp",
			"http://bleu.west.spy.net/~dustin/projects/spytest.xtp",
			"http://bleu.west.spy.net/~dustin/projects/spyjar.xtp"};

		HashMap<URL, String> content=new HashMap<URL, String>();
		
		for(int i=0; i<urls.length; i++) {
			URL u=new URL(urls[i]);
			String s=uw.getContent(u);
			assertNotNull("Didn't get content for " + u, s);
			content.put(u, s);
			// Sleep a bit after we get going.
			if(i>5) {
				Thread.sleep(50);
			}
		}

		// Wait a bit
		Thread.sleep(200);

		for(int i=0; i<urls.length; i++) {
			URL u=new URL(urls[i]);
			assertTrue("Not watching " + u, uw.isWatching(u));

			String s=uw.getContent(u);
			assertNotNull("Didn't get content for " + u + " 2nd time", s);

			// Verify it looks the same.
			String s1=content.get(u);
			assertNotNull("Saved content was null for " + u, s1);

			assertEquals("Second run was different for " + u, s1, s);
			assertSame("Second run was a different instance for " + u, s1, s);
		}
	}

	static class TestURLItem extends URLItem {
		public TestURLItem(URL u) {
			super(u);
		}

		public TestURLItem(URL u, int i) {
			super(u, i);
		}

		@Override
		protected HTTPFetch getFetcher(Map<String, List<String>> headers) {
			return new TestHTTPFetch(getURL(), headers);
		}
		
	}

	static class TestHTTPFetch extends HTTPFetch {
		public TestHTTPFetch(URL u, Map<String, List<String>> head) {
			super(u, head);
		}

		@Override
		public String getData() throws IOException {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return String.valueOf(System.currentTimeMillis());
		}

		@Override
		public long getLastModified() throws IOException {
			return System.currentTimeMillis();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map<String, List<String>> getResponseHeaders() {
			return Collections.EMPTY_MAP;
		}

		@Override
		public int getStatus() throws IOException {
			return 200;
		}
		
	}

	static class TestURLWatcher extends URLWatcher {

		@Override
		protected URLItem getNewURLItem(URL u) {
			return new TestURLItem(u);
		}
		
	}
}
