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
		Thread.sleep(250);
		assertTrue("Didn't start?", tlt.started);
		assertTrue("Didn't finish?", tlt.shutdown);
		assertTrue("Didn't run enough", tlt.runs > 8);
		assertTrue("Ran too many times", tlt.runs < 12);
	}

	public void testNameConstructor() throws Exception {
		TestLoopingThread tlt=new TestLoopingThread("X");
		assertEquals("X", tlt.getName());
	}

	public void testThreadGroupConstructor() throws Exception {
		ThreadGroup tg=new ThreadGroup("Test");
		tg.setDaemon(true);
		TestLoopingThread tlt=new TestLoopingThread(tg, "Y");
		assertEquals("Y", tlt.getName());
		assertSame(tg, tlt.getThreadGroup());
		tg.destroy();
	}

	public void testInterruption() throws Exception {
		TestLoopingThread tlt=new TestLoopingThread();
		tlt.setMsPerLoop(10000);
		tlt.start();
		Thread.sleep(100);
		assertTrue(tlt.started);
		assertFalse(tlt.isInterrupted());
		tlt.interrupt();
		Thread.yield();
		assertTrue(tlt.isInterrupted());
		tlt.requestStop();
		Thread.sleep(100);
		assertTrue(tlt.shutdown);
	}

	private static class TestLoopingThread extends LoopingThread {

		public int runs=0;
		public boolean started=false;
		public boolean shutdown=false;

		public TestLoopingThread() {
			super();
		}
		public TestLoopingThread(String name) {
			super(name);
		}
		public TestLoopingThread(ThreadGroup tg, String name) {
			super(tg, name);
		}

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
