// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 754C9058-1704-44B0-B8EC-02E1D7462A2F

package net.spy.test;

import java.util.HashMap;
import java.util.Map;

import net.spy.util.Instantiator;

import junit.framework.TestCase;

/**
 * Test the instantiator.
 */
public class InstantiatorTest extends TestCase {

	public void testSimple() throws Exception {
		Instantiator<Map> i=new Instantiator<Map>("java.util.HashMap");
		assertTrue(i.getInstance() instanceof HashMap);
		assertSame(i.getInstance(), i.getInstance());
		Instantiator<Map>i2=new Instantiator<Map>("java.util.HashMap");
		assertTrue(i2.getInstance() instanceof HashMap);
		assertNotSame(i2.getInstance(), i.getInstance());
		assertEquals(i.getInstance(), i2.getInstance());
	}

	public void testWithClassLoader() throws Exception {
		Instantiator<Map> i=new Instantiator<Map>("java.util.HashMap",
			getClass().getClassLoader());
		Instantiator<Map> i2=new Instantiator<Map>("java.util.HashMap",
			String.class.getClassLoader());
		assertTrue(i.getInstance() instanceof HashMap);
		assertTrue(i2.getInstance() instanceof HashMap);
		assertNotSame(i.getInstance(), i2.getInstance());
		assertSame(i.getInstance(), i.getInstance());
		assertSame(i2.getInstance(), i2.getInstance());
	}

	public void testCustomInstantiator() throws Exception {
		Instantiator<Map<String, String>> i=new TestInstantiator(1);
		assertTrue(i.getInstance() instanceof HashMap);
		assertEquals(2, i.getInstance().size());
	}

	public void testUninitialized() throws Exception {
		Instantiator<Map<String, String>> i=new TestInstantiator(0);
		try {
			fail("Shouldn't allow me to get anything, got " + i.getInstance());
		} catch(AssertionError e) {
			assertEquals("Instance has not been set.", e.getMessage());
		}
	}

	public void testDoubleInitialized() throws Exception {
		try {
			fail("Shouldn't allow me to get anything, got "
					+ new TestInstantiator(2));
		} catch(AssertionError e) {
			assertEquals("Instance has already been set.", e.getMessage());
		}
	}

	private static final class TestInstantiator
			extends Instantiator<Map<String, String>> {
		public TestInstantiator(int howMany) throws Exception {
			super();
			String name="java.util.HashMap";
			for(int i=0; i<howMany; i++) {
				getLogger().info("Going to make a %s", name);
				setInstance(createInstance(name));
				getInstance().put("a", "1");
				getInstance().put("b", "2");
			}
		}
	}
}
