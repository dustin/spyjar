/*
 * Copyright (c) 2002  Dustin Sallings
 *
 * arch-tag: 096F9FD2-1110-11D9-A6B1-000A957659CC
 */

package net.spy.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test everything.
 */
public class AllTests extends TestSuite {

    /**
     * Get an instance of AllTests.
     */
    public AllTests(String name) {
        super(name);
    }

	/**
	 * Get this test suite.
	 */
	public static Test suite() {
		TestSuite rv=new TestSuite();
		rv.addTest(QuickTests.suite());
		rv.addTest(ThreadPoolTest.suite());
		return(rv);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

}
