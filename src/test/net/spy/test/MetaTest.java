// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: FDD5421B-0ED5-4909-B601-B4ED0EC0B08E

package net.spy.test;

import org.jmock.Mock;

/**
 * Test the test stuff.
 */
public class MetaTest extends BaseMockCase {

	public void testMatcherUnused() {
		Mock m=mock(TestInterface.class);
		m.expects(any()).method("theVal").will(returnValue("X"));
	}

	public void testMatcherUsed() {
		Mock m=mock(TestInterface.class);
		m.expects(any()).method("theVal").will(returnValue("X"));
		assertEquals("X", ((TestInterface)m.proxy()).theVal());
	}

	public void testMatcherUsedTwice() {
		Mock m=mock(TestInterface.class);
		m.expects(any()).method("theVal").will(returnValue("X"));
		assertEquals("X", ((TestInterface)m.proxy()).theVal());
		assertEquals("X", ((TestInterface)m.proxy()).theVal());
	}

	public static interface TestInterface {
		String theVal();
	}
}
