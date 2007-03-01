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

	public Future<V> schedule(Callable<V> c, long d, TimeUnit unit) {
		QueueingFuture rv=new QueueingFuture(c);
		executor.schedule(rv, d, unit);
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

	public Future<V> submit(Callable<V> task) {
		QueueingFuture rv=new QueueingFuture(task);
		executor.execute(rv);
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

}
