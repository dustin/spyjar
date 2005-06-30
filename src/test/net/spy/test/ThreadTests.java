// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 4706B0BB-BA72-4925-BD18-241F2C0BB4C7

package net.spy.test;

import junit.framework.TestCase;

import net.spy.SpyThread;

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
