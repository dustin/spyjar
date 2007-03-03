// Copyright (c) 2007  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 3F26E509-3016-424B-AD0A-AC918D607889

package net.spy.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * CompletionService that honors RetryableCallable instances.
 *
 * @param <V>
 */
public class RetryableExecutorCompletionService<V> 
	implements CompletionService<V> {

	private ExecutorService executor = null;
	BlockingQueue<Future<V>> completionQueue = null;

	public RetryableExecutorCompletionService(ExecutorService e) {
		super();
		completionQueue=new LinkedBlockingQueue<Future<V>>();
		executor=e;
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

	class QueueingFuture extends FutureTask<V> {
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

	class TrackingCallable implements RetryableCallable<V> {

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

		public synchronized void executionComplete(boolean success) {
			callable.executionComplete(success);
			assert future != null : "Future is null";
			completionQueue.add(future);
		}

		public void retryingForException(ExecutionException exception) {
			callable.retryingForException(null);
		}

		public V call() throws Exception {
			assert future != null : "Future is null";
			return callable.call();
		}
	}

}
