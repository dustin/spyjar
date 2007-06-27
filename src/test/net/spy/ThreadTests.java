// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy;

import junit.framework.TestCase;

/**
 * Test some threading stuff.
 */
public class ThreadTests extends TestCase {

	/** 
	 * Test the basics of SpyThread.
	 */
	public void testBasicThread() {
		SpyThread t=new SpyThread("hello");
		assertEquals("hello", t.getName());
	}

}
