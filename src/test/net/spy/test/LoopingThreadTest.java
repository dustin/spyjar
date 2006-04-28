// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 24AE46C8-7A84-4615-AB35-2049516BE457

package net.spy.test;

import net.spy.util.LoopingThread;

import junit.framework.TestCase;

/**
 * Test the looping thread.
 */
public class LoopingThreadTest extends TestCase {

	public void testSimple() throws Exception {
		TestLoopingThread tlt=new TestLoopingThread();
		tlt.setMsPerLoop(10);
		assertFalse(tlt.started);
		tlt.start();
		Thread.sleep(100);
		tlt.requestStop();
		Thread.sleep(100);
		assertTrue("Didn't start?", tlt.started);
		assertTrue("Didn't finish?", tlt.shutdown);
		assertTrue("Didn't run enough", tlt.runs > 8);
		assertTrue("Ran too many times", tlt.runs < 12);
	}

	private static class TestLoopingThread extends LoopingThread {

		public int runs=0;
		public boolean started=false;
		public boolean shutdown=false;
		protected void runLoop() {
			runs++;
		}
		protected void shuttingDown() {
			super.shuttingDown();
			shutdown=true;
		}
		protected void startingUp() {
			super.startingUp();
			started=true;
		}

		
	}
}
