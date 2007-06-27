// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ScheduledExecutorService that executes in-line.  Handy for testing, probably
 * shouldn't be used for much else as complicated blocking scenarios may arise.
 */
public class InlineScheduledExecutorService extends
	AbstractExecutorService implements ScheduledExecutorService {

	public ScheduledFuture<?> schedule(Runnable command, long delay,
			TimeUnit unit) {
		ScheduledFuture<?> rv=new F<Object>(null, unit.toMillis(delay));
		try {
			Thread.sleep(unit.toMillis(delay));
			command.run();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return rv;
	}

	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
			TimeUnit unit) {
		ScheduledFuture<V> rv=null;
		try {
			Thread.sleep(unit.toMillis(delay));
			try {
				rv=new F<V>(callable.call(), unit.toMillis(delay));
			} catch (Exception e) {
				rv=new F<V>(null, e, delay);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return rv;
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
			long initialDelay, long period, TimeUnit unit) {
		throw new RuntimeException("Not supported");
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit) {
		throw new RuntimeException("Not supported");
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return true;
	}

	public boolean isShutdown() {
		return false;
	}

	public boolean isTerminated() {
		return false;
	}

	public void shutdown() {
		// OK
	}

	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
	}

	public void execute(Runnable command) {
		command.run();
	}

	private static class F<V> implements ScheduledFuture<V> {

		private Exception exception=null;
		private V val=null;
		private long delay=0;

		public F(V v, long d) {
			super();
			val=v;
			delay=d;
		}

		public F(Object object, Exception e, long d) {
			super();
			exception=e;
			delay=d;
		}

		public long getDelay(TimeUnit arg0) {
			return TimeUnit.MILLISECONDS.convert(delay, arg0);
		}

		public int compareTo(Delayed arg0) {
			return new Long(delay).compareTo(
					arg0.getDelay(TimeUnit.MILLISECONDS));
		}

		public boolean cancel(boolean arg0) {
			return false;
		}

		public V get() throws InterruptedException, ExecutionException {
			if(exception != null) {
				throw new ExecutionException(exception);
			}
			return val;
		}

		public V get(long arg0, TimeUnit arg1)
			throws InterruptedException, ExecutionException, TimeoutException {
			if(exception != null) {
				throw new ExecutionException(exception);
			}
			return val;
		}

		public boolean isCancelled() {
			return false;
		}

		public boolean isDone() {
			return true;
		}
		
	}
}
