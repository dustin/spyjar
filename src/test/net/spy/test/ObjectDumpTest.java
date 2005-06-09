// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 84ABA577-2594-491E-AB1B-224C0664C5B5

package net.spy.test;

import java.util.Map;

import junit.framework.TestCase;

import net.spy.util.ObjectDump;

/**
 * Excercise ObjectDump.
 */
public class ObjectDumpTest extends TestCase {

	/**
	 * Get an instance of ObjectDumpTest.
	 */
	public ObjectDumpTest(String name) {
		super(name);
	}

	/** 
	 * Test an object dump.
	 * This test primarily exists to exercise ObjectDump, not necessarily
	 * validate any of its values.
	 */
	public void testObjectDump() throws Exception {
		Map m=new java.util.HashMap();
		m.put("aKey", "aValue");
		m.put("myself", m);

		ObjectDump od=new ObjectDump();
		od.dumpObject(m);
	}

	/** 
	 * Test a null object will throw an NPE.
	 */
	public void testNullObjectDump() throws Exception {
		ObjectDump od=new ObjectDump();
		try {
			od.dumpObject(null);
			fail("Expected NPE when dumping null object");
		} catch(NullPointerException e) {
			// pass
		}
	}

}
