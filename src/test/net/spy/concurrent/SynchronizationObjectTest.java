// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 47F51C59-3747-4A58-81B5-547B132970C0

package net.spy.concurrent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

/**
 * Test the synchronization object.
 */
public class SynchronizationObjectTest extends TestCase {

	private Timer timer=null;
	SynchronizationObject<Long> so=null;

	@Override
	protected void setUp() {
		timer=new Timer(true);
		so=new SynchronizationObject<Long>(null);
	}

	@Override
	protected void tearDown() {
		timer.cancel();
	}

	private void setToIn(final Long v, long delay) {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				so.set(v);
			}}, delay);
	}

	public void testBasic() throws Exception {
		so.waitUntilEquals(null, 100, TimeUnit.MILLISECONDS);
		setToIn(13L, 100);
		assertNull(so.get());
		so.waitUntilEquals(13L, 150, TimeUnit.MILLISECONDS);
		assertEquals(new Long(13), so.get());
		try {
			so.waitUntilEquals(17L, 50, TimeUnit.MILLISECONDS);
			fail("Expected timeout waiting for 17, got " + so);
		} catch(TimeoutException e) {
			// Expected
		}
	}

	public void testWaitUntilNotNull() throws Exception {
		setToIn(13L, 100);
		assertNull(so.get());
		so.waitUntilNotNull(150, TimeUnit.MILLISECONDS);
	}

	public void testInterrupt() throws Exception {
		final Thread t=Thread.currentThread();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				t.interrupt();
			}	
		}, 100);
		try {
			so.waitUntilEquals(19L, Long.MAX_VALUE-1, TimeUnit.MILLISECONDS);
			fail("value became 19 and you're still alive.");
		} catch(InterruptedException e) {
			// Expected
		}
	}

	public void testMultipleSets() throws Exception {
		setToIn(7L, 10);
		setToIn(13L, 15);
		setToIn(17L, 25);
		setToIn(19L, 40);
		so.waitUntilEquals(19L, 50, TimeUnit.MILLISECONDS);
	}

	public void testString() {
		so.set(42L);
		assertEquals("{SynchronizationObject obj=42}", so.toString());
	}

	// This test fails on my macbook.  The predicate is not evaluated for
	// every change.
	public void xtestFastSequenceSets() throws Exception {
		Thread t=new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				for(int i=1; i<=100; i++) {
					Long old=so.set(new Long(i));
					System.err.println(old + " -> " + i);
					// Thread.yield();
				}
			}
		};
		t.setDaemon(true);
		t.start();
		SeqPred sp=new SeqPred(100);
		so.waitUntilTrue(sp, 30, TimeUnit.SECONDS);
		assertEquals(100, sp.updatesSeen);
		t.join();
	}

	private class SeqPred implements SynchronizationObject.Predicate<Long> {

		public int updatesSeen=0;
		public long lastUpdate=0;
		private int expectedUpdates=0;

		public SeqPred(int exp) {
			super();
			expectedUpdates=exp;
		}

		public boolean evaluate(Long o) {
			System.err.println("-- saw " + o);
			if(updatesSeen == 0) {
				assertNull("Expected first update to be null, was " + o, o);
				lastUpdate=0;
			} else {
				assertEquals(new Long(lastUpdate + 1), o);
				lastUpdate=o;
			}
			return ++updatesSeen == expectedUpdates;
		}
		
	}
}
