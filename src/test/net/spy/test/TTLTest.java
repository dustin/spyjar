// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 6EFDD9FC-D8E6-4465-B1DE-4109288A0790

package net.spy.test;

import junit.framework.TestCase;
import net.spy.db.DBTTL;
import net.spy.util.TTL;
import net.spy.util.TTLMonitor;

/**
 * Test the TTL implementation.
 */
public class TTLTest extends TestCase {

	private TTLMonitor mon=null;

	/**
	 * Get an instance of TTLTest.
	 */
	public TTLTest(String name) {
		super(name);
	}

	protected void setUp() {
		mon=TTLMonitor.getTTLMonitor();
	}

	protected void tearDown() {
		mon.shutdown();
	}

	/** 
	 * Test the basic TTL monitor stuff.
	 */
	public void testBasicTTL() throws Exception {
		String.valueOf(mon);
		long timeout=500;

		Object theObject=new Object();

		TTL testTTL1=new TTL(timeout, theObject);
		testTTL1.setMaxReports(2);
		testTTL1.setReportInterval(250);
		String.valueOf(testTTL1);

		TTL testTTL2=new TTL(timeout);
		String.valueOf(testTTL2);
		testTTL2.setExtraInfo(theObject);

		TTL testTTL3=new TTL(timeout);
		String.valueOf(testTTL3);

		// Add the monitors
		mon.add(testTTL1);
		mon.add(testTTL2);
		mon.add(testTTL3);
		mon.add(new TTL(100));

		// Begin assertions
		assertEquals(timeout, testTTL1.getTTL());
		assertFalse(testTTL1.hasReported());
		assertFalse(testTTL2.hasReported());
		assertFalse(testTTL1.isClosed());
		assertFalse(testTTL2.isClosed());
		assertFalse(testTTL1.isExpired());
		assertFalse(testTTL2.isExpired());
		assertSame(theObject, testTTL1.getExtraInfo());
		assertSame(theObject, testTTL2.getExtraInfo());

		// Close one of them.
		testTTL3.close();

		// Wait for some ttls to expire
		Thread.sleep(700);
		assertTrue(testTTL1.isExpired());
		assertTrue(testTTL2.isExpired());
		assertTrue(testTTL1.hasReported());
		assertTrue(testTTL2.hasReported());
		assertFalse(testTTL3.hasReported());
		assertFalse(testTTL1.isClosed());
		assertFalse(testTTL2.isClosed());

		// Wait for some more ttls
		Thread.sleep(350);
		assertTrue(testTTL1.hasReported());
		assertTrue(testTTL2.hasReported());
		assertFalse(testTTL3.hasReported());
		assertTrue(testTTL1.isClosed());
		assertFalse(testTTL2.isClosed());
	}

	/** 
	 * Test DBTTL as well.
	 */
	public void testDBTTL() throws Exception {
		DBTTL ttl1=new DBTTL(100);
		DBTTL ttl2=new DBTTL(100, new Object());
		String.valueOf(ttl1);
		String.valueOf(ttl2);
		mon.add(ttl1);
		mon.add(ttl2);
		Thread.sleep(250);
	}

	/** 
	 * TTL Test with a missing message.
	 */
	public void testWithMissingMessages() throws Exception {
		mon.add(new TestTTLBad1(100));
		mon.add(new TestTTLBad2(100));
		Thread.sleep(250);
	}

	/** 
	 * Test missing message lookup.
	 */
	public void testMissingMessages() throws Exception {
		TestTTLBad1 ttl=new TestTTLBad1(100);
		String firstRv=ttl.getMessageFromBundle("x", "y", "z");

		assertTrue(firstRv.startsWith(
			"ResourceBundle not found while reporting TTL expiration:  x"));

		String secondRv=ttl.getMessageFromBundle("net.spy.util.messages",
			"y", "z");
		assertTrue(secondRv.startsWith(
			"Resource not found while reporting TTL expiration:  y"));
	}

	private static final class TestTTLBad1 extends TTL {
		public TestTTLBad1(long delay) {
			super(delay);
		}

		public String getMessageFromBundle(String bundleName,
			String msgNoArg, String msgWithArg) {
			return(super.getMessageFromBundle(bundleName,
				msgNoArg, msgWithArg));
		}

		protected void doReport() {
			reportWithFormat(getMessageFromBundle("net.spy.nonexistent",
				"ttl.msg", "ttl.msg.witharg"));
		}
	}

	private static final class TestTTLBad2 extends TTL {
		public TestTTLBad2(long delay) {
			super(delay);
		}

		protected void doReport() {
			reportWithFormat(getMessageFromBundle("net.spy.util.messages",
				"ttlBAD.msg", "ttlBAD.msg.witharg"));
		}
	}

}
