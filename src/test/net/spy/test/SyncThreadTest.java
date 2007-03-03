// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: A35D2916-2B94-42BF-8479-B726E5C267C9

package net.spy.test;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

import junit.framework.TestCase;

/**
 * Test the sync thread.
 */
public class SyncThreadTest extends TestCase {

	public void testInterruption() throws Throwable {
		CyclicBarrier barrier=new CyclicBarrier(2);
		SyncThread<Object> st=new SyncThread<Object>(barrier,
				new Callable<Object>() {
					public Object call() throws Exception {
						return "X";
					}});
		st.interrupt();
		try {
			Object x=st.getResult();
			fail("Get should've failed, got " + x);
		} catch(InterruptedException e) {
			// pass
		}
	}

	public void testCountingSame() throws Throwable {
		final Object o=new Object();
		int n=SyncThread.getDistinctResultCount(50, new Callable<Object>() {
			public Object call() throws Exception {
				return o;
			}});
		assertEquals(1, n);
	}

	public void testCountingDifferent() throws Throwable {
		int n=SyncThread.getDistinctResultCount(50, new Callable<Object>() {
			public Object call() throws Exception {
				return new Object();
			}});
		assertEquals(50, n);
	}
}
