// Copyright (c) 2007  Dustin Sallings <dustin@spy.net>
//
// arch-tag: FF8F95CC-B612-41CE-9E6B-093BF426F630
package net.spy.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Completion service that allows completion tracking on scheduled items as
 * well as 
 *
 * @param <V>
 */
public class ScheduledExecutorCompletionService<V> implements
		ScheduledCompletionService<V> {

	private ScheduledExecutorService executor = null;
	BlockingQueue<Future<V>> completionQueue = null;

	public ScheduledExecutorCompletionService(ScheduledExecutorService ex) {
		super();
		executor = ex;
		completionQueue=new LinkedBlockingQueue<Future<V>>();
	}

	public Future<V> schedule(Callable<V> c, long d, TimeUnit unit) {
		Future<V> rv=null;
		if(c instanceof RetryableCallable) {
			TrackingCallable tc=new TrackingCallable((RetryableCallable<V>)c);
			// Lock the callable before submitting to ensure it can know its
			// Future before it attempts to run
			synchronized(tc) {
				rv=executor.schedule(tc, d, unit);
				tc.setFuture(rv);
			}
		} else {
			rv=new QueueingFuture(c);
			executor.schedule((Runnable)rv, d, unit);
		}
		return rv;
	}

	public Future<?> schedule(Runnable r, long d, TimeUnit unit) {
		QueueingFuture rv=new QueueingFuture(r, null);
		executor.schedule(rv, d, unit);
		return rv;
	}

	public Future<V> poll() {
		return completionQueue.poll();
	}

	public Future<V> poll(long timeout, TimeUnit unit)
			throws InterruptedException {
		return completionQueue.poll(timeout, unit);
	}

	public Future<V> submit(Callable<V> c) {
		Future<V> rv=null;
		if(c instanceof RetryableCallable) {
			TrackingCallable tc=new TrackingCallable((RetryableCallable<V>)c);
			// Lock the callable before submitting to ensure it can know its
			// Future before it attempts to run
			synchronized(tc) {
				rv=executor.submit(tc);
				tc.setFuture(rv);
			}
		} else {
			rv=new QueueingFuture(c);
			executor.submit((Runnable)rv);
		}
		return rv;
	}

	public Future<V> submit(Runnable task, V result) {
		QueueingFuture rv=new QueueingFuture(task, result);
		executor.execute(rv);
		return rv;
	}

	public Future<V> take() throws InterruptedException {
		return completionQueue.take();
	}

	private class QueueingFuture extends FutureTask<V> {
		QueueingFuture(Callable<V> c) {
			super(c);
		}

		QueueingFuture(Runnable t, V r) {
			super(t, r);
		}

		protected void done() {
			completionQueue.add(this);
		}
	}

	private class TrackingCallable implements RetryableCallable<V> {

		private RetryableCallable<V> callable=null;
		private Future<V> future=null;

		public TrackingCallable(RetryableCallable<V> c) {
			super();
			callable=c;
		}

		public void setFuture(Future<V> f) {
			future=f;
			assert future != null : "Future is null";
		}

		public long getRetryDelay() {
			return callable.getRetryDelay();
		}

		public void givingUp() {
			callable.givingUp();
			assert future != null : "Future is null";
			completionQueue.add(future);
		}

		public void retrying() {
			callable.retrying();
		}

		public synchronized V call() throws Exception {
			assert future != null : "Future is null";
			V rv=callable.call();
			completionQueue.add(future);
			return rv;
		}
		
	}
}
