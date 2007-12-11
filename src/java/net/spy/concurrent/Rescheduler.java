// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.SpyObject;
import net.spy.concurrent.SynchronizationObject.Predicate;

/**
 * ScheduledExecutorService wrapper that allows RetryableCallable objects to
 * be retried upon failure.
 *
 * Note that while this does back an existing ScheduledExecutorService, any
 * Callables sent directly into that service will not be eligible for retry.
 */
public class Rescheduler extends SpyObject implements ScheduledExecutorService {

	private final ScheduledExecutorService executor;

	/**
	 * Get an instance of Rescheduler with the given backing
	 * ScheduledExecutorService.
	 *
	 * @param x the ScheduledExecutorService that will be responsible execution
	 */
	public Rescheduler(ScheduledExecutorService x) {
		super();
		executor=x;
	}

	@SuppressWarnings("unchecked") // discarding future type
	void examineCompletion(final FutureFuture future) {
		try {
			boolean set=false;
			Object result=null;
			while(!set) {
				try {
					result=future.getCurrentFuture().get();
					set=true;
				} catch(InterruptedException e) {
					getLogger().info("Interrupted.  Retrying", e);
				}
			}
			future.setResult(result);
			future.callable.onComplete(true, result);
		} catch(CancellationException e) {
			future.setCancelled();
		} catch (ExecutionException e) {
			assert future != null : "Lost the future";
			future.addException(e);
			long nextTime=future.callable.getRetryDelay();
			if(!future.isCancelled()) {
				if(nextTime >= 0) {
					future.callable.onExecutionException(null);
					future.clearCurrentFuture();
					scheduleFutureFuture(future,
							nextTime, TimeUnit.MILLISECONDS);
				} else {
					future.callable.onComplete(false, future.exceptions);
					assert future.exceptions != null : "Exceptions is null";
					future.setResult(future.exceptions);
				}
			}
		}
	}

	public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
		return executor.schedule(r, delay, unit);
	}

	private <T> void scheduleFutureFuture(final FutureFuture<T> ff,
			long delay, TimeUnit unit) {
		FutureTask<T> ft=new FutureTask<T>(ff.callable) {
			@Override
			protected void done() {
				examineCompletion(ff);
			}
		};
		executor.schedule(ft, delay, unit);
		ff.setCurrentFuture(ft);
	}

	/**
	 * Process the given callable.  If this is a RetryableCallable, it may be
	 * retried if execution fails.
	 */
	public <T> ScheduledFuture<T> schedule(Callable<T> c, long delay,
			TimeUnit unit) {
		ScheduledFuture<T> rv=null;
		if(c instanceof RetryableCallable) {
			RetryableCallable<T> rc=(RetryableCallable<T>)c;
			final ScheduledFutureFuture<T> ff=new ScheduledFutureFuture<T>(rc,
					TimeUnit.MILLISECONDS.convert(delay, unit));

			scheduleFutureFuture(ff, delay, unit);
			rv=ff;
		} else {
			rv=executor.schedule(c, delay, unit);
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

	public <T> List<Future<T>> invokeAll(Collection<Callable<T>> callables)
			throws InterruptedException {
		List<Future<T>> rv=new ArrayList<Future<T>>(callables.size());
		RetryableExecutorCompletionService<T> ecs=
			new RetryableExecutorCompletionService<T>(this);
		for(Callable<T> c : callables) {
			rv.add(ecs.submit(c));
		}
		for(int i=0; i<callables.size(); i++) {
			ecs.take();
		}
		return rv;
	}

	public <T> List<Future<T>> invokeAll(Collection<Callable<T>> callables,
			long timeout, TimeUnit unit) throws InterruptedException {

		long end=System.currentTimeMillis()+TimeUnit.MILLISECONDS.convert(
				timeout, unit);

		List<Future<T>> rv=new ArrayList<Future<T>>(callables.size());
		RetryableExecutorCompletionService<T> ecs=
			new RetryableExecutorCompletionService<T>(this);
		for(Callable<T> c : callables) {
			rv.add(ecs.submit(c));
		}
		for(int i=0; i<callables.size(); i++) {
			long now=System.currentTimeMillis();
			long towait=end-now;
			if(towait > 0) {
				ecs.poll(towait, TimeUnit.MILLISECONDS);
			}
		}
		return rv;
	}

	public <T> T invokeAny(Collection<Callable<T>> callables)
			throws InterruptedException, ExecutionException {
		List<Future<T>> futures=new ArrayList<Future<T>>(callables.size());
		RetryableExecutorCompletionService<T> ecs=
			new RetryableExecutorCompletionService<T>(this);
		for(Callable<T> c : callables) {
			futures.add(ecs.submit(c));
		}

		Collection<ExecutionException> exceptions=
			new ArrayList<ExecutionException>();

		// Everything's submitted.  Wait for stuff to finish.
		boolean foundResult=false;
		T rv=null;
		while(!foundResult && !futures.isEmpty()) {
			Future<T> f=ecs.take();
			futures.remove(f);
			try {
				rv=f.get();
				foundResult=true;
			} catch(ExecutionException e) {
				exceptions.add(e);
			}
		}
		if(!foundResult) {
			throw new CompositeExecutorException(exceptions);
		}
		for(Future<T> f : futures) {
			f.cancel(true);
		}
		return rv;
	}

	public <T> T invokeAny(Collection<Callable<T>> callables, long timeout,
			TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException {

		long end=System.currentTimeMillis() +
			TimeUnit.MILLISECONDS.convert(timeout, unit);

		List<Future<T>> futures=new ArrayList<Future<T>>(callables.size());
		RetryableExecutorCompletionService<T> ecs=
			new RetryableExecutorCompletionService<T>(this);
		for(Callable<T> c : callables) {
			futures.add(ecs.submit(c));
		}

		Collection<ExecutionException> exceptions=
			new ArrayList<ExecutionException>();

		// Everything's submitted.  Wait for stuff to finish.
		boolean foundResult=false;
		T rv=null;
		while(!foundResult && !futures.isEmpty()) {
			long now=System.currentTimeMillis();
			long towait=end-now;
			Future<T> f=ecs.poll(towait, TimeUnit.MILLISECONDS);
			if(f == null) {
				throw new TimeoutException(
						"Timed out waiting " + towait + "ms of "
						+ timeout + unit.name());
			} else {
				futures.remove(f);
				assert f.isDone() : "Future is not done";
				try {
					rv=f.get();
					foundResult=true;
				} catch(ExecutionException e) {
					exceptions.add(e);
				}
			}
		}
		if(!foundResult) {
			throw new CompositeExecutorException(exceptions);
		}
		for(Future<T> f : futures) {
			f.cancel(true);
		}
		return rv;
	}

	public void shutdown() {
		executor.shutdown();
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public List<Runnable> shutdownNow() {
		return executor.shutdownNow();
	}

	/**
	 * Process the given callable.  If this is a RetryableCallable, it may be
	 * retried if execution fails.
	 */
	public <T> Future<T> submit(Callable<T> c) {
		Future<T> rv=null;
		if(c instanceof RetryableCallable) {
			RetryableCallable<T> rc=(RetryableCallable<T>)c;
			final FutureFuture<T> ff=new FutureFuture<T>(rc);
			FutureTask<T> ft=new FutureTask<T>(rc) {
				@Override
				protected void done() {
					examineCompletion(ff);
				}
			};
			executor.submit(ft);
			ff.setCurrentFuture(ft);
			rv=ff;
		} else {
			rv=executor.submit(c);
		}
		return rv;
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

	static class FutureFuture<T> implements Future<T> {

		final RetryableCallable<T> callable;
		final Object defaultObj=new Object();
		final Object cancelObj=new Object();
		CompositeExecutorException exceptions=null;
		private final SynchronizationObject<Future<T>> futureSync=
			new SynchronizationObject<Future<T>>(null);
		private final SynchronizationObject<Object> sync=
			new SynchronizationObject<Object>(defaultObj);

		public FutureFuture(RetryableCallable<T> c) {
			super();
			callable=c;
		}

		public void addException(ExecutionException e) {
			if(exceptions == null) {
				exceptions=new CompositeExecutorException(e);
			} else {
				exceptions.addException(e);
			}
		}

		public void setResult(Object t) {
			sync.set(t);
		}

		public Future<T> getCurrentFuture() throws InterruptedException {
			try {
				futureSync.waitUntilNotNull(
						Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException(e);
			}
			Future<T> f=futureSync.get();
			assert f != null : "Current future fetch failed";
			return f;
		}

		public void clearCurrentFuture() {
			setCurrentFuture(null);
		}

		public void setCurrentFuture(Future<T> to) {
			futureSync.set(to);
		}

		public void setCancelled() {
			sync.set(cancelObj);
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean rv=false;
			setCancelled();
			Future<T> f=futureSync.get();
			if(f != null) {
				rv=f.cancel(mayInterruptIfRunning);
			}
			return rv;
		}

		public T get() throws InterruptedException, ExecutionException {
			try {
				return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException(
						"Infinite sleep over.  The end is near", e);
			}
		}

		public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
			sync.waitUntilTrue(new Predicate<Object>(){
				public boolean evaluate(Object o) {
					return o != defaultObj;
				}}, timeout, unit);
			Object o=sync.get();
			if(o == cancelObj) {
				throw new CancellationException("Cancelled");
			} else if(o instanceof CompositeExecutorException) {
				throw (ExecutionException)o;
			} else {
				@SuppressWarnings("unchecked")
				T rv=(T)o;
				return rv;
			}
		}

		public boolean isCancelled() {
			return sync.get() == cancelObj;
		}

		public boolean isDone() {
			return sync.get() != defaultObj;
		}
	}

	static class ScheduledFutureFuture<T> extends FutureFuture<T>
		implements ScheduledFuture<T> {

		private final long when;

		public ScheduledFutureFuture(RetryableCallable<T> c, long delay) {
			super(c);
			when=System.currentTimeMillis()+delay;
		}

		public long getDelay(TimeUnit unit) {
			return TimeUnit.MILLISECONDS.convert(
					when-System.currentTimeMillis(), unit);
		}

		public int compareTo(Delayed d) {
			return new Long(getDelay(TimeUnit.MILLISECONDS))
				.compareTo(d.getDelay(TimeUnit.MILLISECONDS));
		}

	}
}
