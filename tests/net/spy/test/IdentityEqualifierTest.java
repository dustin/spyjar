// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 12B3573D-1110-11D9-B93C-000A957659CC

package net.spy.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.IdentityEqualifier;

/**
 * Test the IdentityEqualifier.
 */
public class IdentityEqualifierTest extends TestCase {

	/**
	 * Get an instance of IdentityEqualifierTest.
	 */
	public IdentityEqualifierTest(String name) {
		super(name);
	}

	/*
	 * Get this test.
	 */
	public static Test suite() {
		return new TestSuite(IdentityEqualifierTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Test basic identity equalifier functionality.
	 */
	public void testIdentityEqualifier() {
		String test1=new String("Test");
		String test2=new String("Test");
		String test3=test1;
		String test4=new String(test1);
		IdentityEqualifier ie1=new IdentityEqualifier(test1);
		IdentityEqualifier ie2=new IdentityEqualifier(test2);
		IdentityEqualifier ie3=new IdentityEqualifier(test3);
		IdentityEqualifier ie4=new IdentityEqualifier(test4);

		assertEquals("=test1+test1", test1, test1);
		assertEquals("=test1+test1", ie1, test1);

		assertEquals("=test1+test2", test1, test2);
		assertNotSame("=test1+test2", test1, test2);
		assertFalse("=test1+test2", ie1.equals(test2));
		assertFalse("=test1+test2", ie1.equals(ie2));

		assertEquals("=test1+test3", test1, test3);
		assertSame("=test1+test3", test1, test3);
		assertNotSame("=test1+test3", ie1, ie3);
		assertEquals("=test1+test3", ie1, test3);
		assertEquals("=test1+test3", ie1, ie3);

		assertEquals("=test1+test4", test1, test4);
		assertNotSame("=test1+test4", test1, test4);
		assertNotSame("=test1+test4", ie1, ie4);
		assertFalse("=test1+test4", ie1.equals(test4));
		assertFalse("=test1+test4", ie1.equals(ie4));
	}

}
