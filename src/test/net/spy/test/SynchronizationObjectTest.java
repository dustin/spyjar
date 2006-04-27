// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 47F51C59-3747-4A58-81B5-547B132970C0

package net.spy.test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.util.SynchronizationObject;

import junit.framework.TestCase;

/**
 * Test the synchronization object.
 */
public class SynchronizationObjectTest extends TestCase {

	private Timer timer=null;
	private SynchronizationObject<Long> so=null;

	protected void setUp() {
		timer=new Timer(true);
		so=new SynchronizationObject<Long>(null);
	}

	protected void tearDown() {
		timer.cancel();
	}

	private void setToIn(final Long v, long delay) {
		timer.schedule(new TimerTask() {
			public void run() {
				so.set(v);
			}}, delay);
	}

	public void testBasic() throws Exception {
		so.waitUntilEquals(null, 100, TimeUnit.MILLISECONDS);
		setToIn(13L, 100);
		assertNull(so.get());
		so.waitUntilEquals(13L, 150, TimeUnit.MILLISECONDS);
		assertEquals(13L, so.get());
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
}
