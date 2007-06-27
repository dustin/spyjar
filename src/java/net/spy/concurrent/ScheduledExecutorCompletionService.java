// Copyright (c) 2007  Dustin Sallings <dustin@spy.net>
package net.spy.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Completion service that allows completion tracking on scheduled items as
 * well as 
 *
 * @param <V>
 */
public class ScheduledExecutorCompletionService<V>
	extends RetryableExecutorCompletionService<V>
	implements ScheduledCompletionService<V> {

	private final ScheduledExecutorService executor;

	public ScheduledExecutorCompletionService(ScheduledExecutorService ex) {
		super(ex);
		executor = ex;
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

}
