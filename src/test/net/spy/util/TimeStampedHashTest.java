// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import junit.framework.TestCase;

/**
 * Test the timestamped hash.
 */
public class TimeStampedHashTest extends TestCase {

	/**
	 * Get an instance of TimeStampedHashTest.
	 */
	public TimeStampedHashTest(String name) {
		super(name);
	}

	/** 
	 * Test the basic hashtable operations.
	 */
	public void testOperations() throws Exception {
		TimeStampedHashMap<String, String> m
			=new TimeStampedHashMap<String, String>();
		long originalTimestamp=m.getTimestamp();
		assertEquals(originalTimestamp, m.getLastGet());
		assertEquals(originalTimestamp, m.getLastPut());

		assertEquals(0, m.getHits());
		assertEquals(0, m.getMisses());
		assertEquals(0, m.getWatermark());
		assertEquals(0, m.getNumPuts());

		Thread.sleep(50);

		m.put("test", "blah");
		assertEquals(m.getTimestamp(), m.getLastPut());
		assertFalse(originalTimestamp == m.getTimestamp());
		assertEquals(originalTimestamp, m.getLastGet());
		assertEquals(1, m.getNumPuts());

		Thread.sleep(50);

		assertEquals("blah", m.get("test"));
		assertEquals(m.getTimestamp(), m.getLastGet());
		assertFalse(m.getTimestamp() == m.getLastPut());
		assertEquals(1, m.getHits());
		assertEquals(0, m.getMisses());

		Thread.sleep(50);

		long ts=m.getTimestamp();
		assertNull(m.get("missing"));
		assertEquals(m.getTimestamp(), m.getLastGet());
		assertFalse(m.getTimestamp() == ts);
		assertEquals(1, m.getHits());
		assertEquals(1, m.getMisses());

		ts=m.getTimestamp();
		long putAge=m.getPutAge();
		long getAge=m.getGetAge();
		long useAge=m.getUseAge();

		Thread.sleep(50);
		assertTrue(m.getPutAge() - putAge >= 50);
		assertTrue(m.getGetAge() - getAge >= 50);
		assertTrue(m.getUseAge() - useAge >= 50);
	}

}
