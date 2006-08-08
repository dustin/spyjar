// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 46CB8A25-0D4F-4987-85E9-EE8B8CB37DB3

package net.spy.concurrent;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.SpyThread;
import net.spy.util.RingBuffer;

/**
 * ScheduledExecutorService wrapper that allows RetryableCallable objects to
 * be retried upon failure.
 * 
 * Note that while this does back an existing ScheduledExecutorService, any
 * Callables sent directly into that service will not be eligible for retry.
 */
public class Rescheduler extends SpyThread implements ScheduledExecutorService {

	private ScheduledExecutorService executor;
	private PriorityBlockingQueue<WatchingTuple> queue;
	private volatile boolean shutdown=false;

	/**
	 * Get an instance of Rescheduler with the given backing
	 * ScheduledExecutorService.
	 * 
	 * @param x the ScheduledExecutorService that will be responsible execution
	 */
	public Rescheduler(ScheduledExecutorService x) {
		super("Rescheduler");
		executor=x;
		queue=new PriorityBlockingQueue<WatchingTuple>();
		start();
	}

	public void run() {
		while(!isShutdown()) {
			WatchingTuple wt=null;
			try {
				wt=queue.take();
				getLogger().debug("Got %s", wt);
				// Wait for completion
				Object o=wt.currentFuture.get();
				wt.futureFuture.sync.set(o);
			} catch (InterruptedException e) {
				if(!isShutdown()) {
					getLogger().info("Someone interrupted us while waiting.", e);
				}
			} catch (ExecutionException e) {
				assert wt != null : "Lost my watch?";
				getLogger().debug("Execution of %s failed.", wt.callable, e);
				if(wt.e == null) {
					wt.e=new CompositeExecutorException(e);
				} else {
					wt.e.exceptions.add(e);
				}

				long retryDelay=wt.callable.getRetryDelay();
				if(retryDelay >= 0) {
					getLogger().debug("Scheduling %s for retry", wt.callable);
					wt.callable.retrying();
					ScheduledFuture<?> f=executor.schedule(wt.callable,
							retryDelay, TimeUnit.MILLISECONDS);
					queue.put(new WatchingTuple(f, wt.callable,
							wt.futureFuture, wt.e));
				} else {
					getLogger().info("No retry for %s", wt.callable);
					getLogger().debug("Fatal error in RetryableCallable", wt.e);
					wt.callable.givingUp();
					wt.futureFuture.sync.set(wt.e);
				}
			}
		}
		getLogger().info("Shut down.  Exiting");
	}

	public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
		return executor.schedule(r, delay, unit);
	}

	/**
	 * Process the given callable.  If this is a RetryableCallable, it may be
	 * retried if execution fails.
	 */
	public <V> ScheduledFuture<V> schedule(Callable<V> c, long delay,
			TimeUnit unit) {
		ScheduledFuture<V> rv=executor.schedule(c, delay, unit);
		if(c instanceof RetryableCallable) {
			getLogger().debug("Scheduling a retryable:  %s", c);
			FutureFuture<V> ff=new FutureFuture<V>(rv, unit.toMillis(delay));
			queue.put(new WatchingTuple(rv, (RetryableCallable<?>) c, ff));
			rv=ff;
		}
		return rv;
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long arg1,
			long arg2, TimeUnit arg3) {
		return executor.scheduleAtFixedRate(r, arg1, arg2, arg3);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable r, long arg1,
			long arg2, TimeUnit arg3) {
		return executor.scheduleWithFixedDelay(r, arg1, arg2, arg3);
	}

	public boolean awaitTermination(long arg0, TimeUnit arg1)
			throws InterruptedException {
		return executor.awaitTermination(arg0, arg1);
	}

	public <T> List<Future<T>> invokeAll(Collection<Callable<T>> arg0)
			throws InterruptedException {
		return executor.invokeAll(arg0);
	}

	public <T> List<Future<T>> invokeAll(Collection<Callable<T>> arg0,
			long arg1, TimeUnit arg2) throws InterruptedException {
		return executor.invokeAll(arg0, arg1, arg2);
	}

	public <T> T invokeAny(Collection<Callable<T>> arg0)
			throws InterruptedException, ExecutionException {
		return executor.invokeAny(arg0);
	}

	public <T> T invokeAny(Collection<Callable<T>> arg0, long arg1,
			TimeUnit arg2) throws InterruptedException, ExecutionException,
			TimeoutException {
		return executor.invokeAny(arg0, arg1, arg2);
	}

	public void shutdown() {
		executor.shutdown(); 
		shutdown=true;
		interrupt();
	}

	public boolean isShutdown() {
		return shutdown || executor.isShutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public List<Runnable> shutdownNow() {
		List<Runnable> rv=executor.shutdownNow();
		shutdown=true;
		interrupt();
		return rv;
	}

	/**
	 * Process the given callable.  If this is a RetryableCallable, it may be
	 * retried if execution fails.
	 */
	public <T> Future<T> submit(Callable<T> c) {
		return schedule(c, 0, TimeUnit.SECONDS);
	}

	public Future<?> submit(Runnable arg0) {
		return executor.submit(arg0);
	}

	public <T> Future<T> submit(Runnable arg0, T arg1) {
		return executor.submit(arg0, arg1);
	}

	public void execute(Runnable arg0) {
		executor.execute(arg0);
	}

	private static class CompositeExecutorException extends ExecutionException {
		Collection<ExecutionException> exceptions=null;
		public CompositeExecutorException(ExecutionException e) {
			super("Too many failures");
			exceptions=new RingBuffer<ExecutionException>(10);
			exceptions.add(e);
		}

		public void printStackTrace(PrintStream p) {
			super.printStackTrace(p);
			for(ExecutionException e : exceptions) {
				try {
					p.write("Also caused by: ".getBytes());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace(p);
			}
		}

		public void printStackTrace(PrintWriter p) {
			super.printStackTrace(p);
			for(ExecutionException e : exceptions) {
				p.print("Also caused by: ");
				e.printStackTrace(p);
			}
		}
	}

	private static class WatchingTuple implements Comparable<WatchingTuple> {

		public CompositeExecutorException e=null;
		public ScheduledFuture<?> currentFuture=null;
		public RetryableCallable<?> callable=null;
		public FutureFuture<?> futureFuture=null;

		public WatchingTuple(ScheduledFuture<?> f, RetryableCallable<?> c,
				FutureFuture<?> ff, CompositeExecutorException exception) {
			super();
			currentFuture=f;
			callable=c;
			futureFuture=ff;
			e=exception;
		}

		public WatchingTuple(ScheduledFuture<?> f, RetryableCallable<?> c,
				FutureFuture<?> ff) {
			this(f, c, ff, null);
		}

		public int compareTo(WatchingTuple wt) {
			return currentFuture.compareTo(wt.currentFuture);
		}

	}

	private static class FutureFuture<T> implements ScheduledFuture<T> {

		private Object defaultObj=null;
		private SynchronizationObject<Object> sync=null;
		private Future<T> currentFuture=null;
		private long delay=0;

		public FutureFuture(Future<T> f, long d) {
			super();
			currentFuture=f;
			delay=d;
			defaultObj=new Object();
			sync=new SynchronizationObject<Object>(defaultObj);
		}

		public long getDelay(TimeUnit unit) {
			return delay;
		}

		public int compareTo(Delayed d) {
			return new Long(delay).compareTo(
					d.getDelay(TimeUnit.MILLISECONDS));
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean rv=currentFuture.cancel(mayInterruptIfRunning);
			if(rv) {
				sync.set(null);
			}
			return rv;
		}

		public T get() throws InterruptedException, ExecutionException {
			try {
				return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException(
						"Timeout on infinite sleep.  World over?");
			}
		}

		@SuppressWarnings("unchecked")
		public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
			sync.waitUntilTrue(new SynchronizationObject.Predicate<Object>() {
				public boolean evaluate(Object o) {
					return o != defaultObj;
				}
			}, timeout, unit);
			Object o=sync.get();
			T rv=null;
			if(o instanceof CompositeExecutorException) {
				throw (ExecutionException)o;
			} else {
				rv=(T)o;
			}
			return rv;
		}

		public boolean isCancelled() {
			return currentFuture.isCancelled();
		}

		public boolean isDone() {
			return defaultObj != sync.get();
		}
		
	}
}
