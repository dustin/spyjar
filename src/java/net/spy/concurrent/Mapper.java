package net.spy.concurrent;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.SpyObject;

/**
 * Map a collection of objects with a given transformer.
 */
public class Mapper<F, T> extends SpyObject {

	private final ExecutorService executor;

	/**
	 * Construct a Mapper backed by an in-place executor.
	 */
	public Mapper() {
		this(new DummyExecutor());
	}

	/**
	 * Construct a Mapper backed by the given executor service.
	 */
	public Mapper(ExecutorService x) {
		super();
		executor=x;
	}

	/**
	 * Transform the given collection using the given transformer.
	 *
	 * @param trans the transformer
	 * @param in the collection to transform
	 * @return a list of transformed values in the same order in which they
	 *   were iterated by the given collection
	 * @throws InterruptedException if we're interrupted while processing
	 */
	public List<T> transform(Transformer<F,T> trans,
			Collection<? extends F> in) throws InterruptedException {
		List<T> rv=new ArrayList<T>(in.size());
		List<Future<T>> fs=executor.invokeAll(
				new TransformingCollection<F,T>(trans, in));
		for(Future<T> f : fs) {
			try {
				rv.add(f.get());
			} catch (ExecutionException e) {
				throw new RuntimeException("Error processing get", e);
			}
		}
		return rv;
	}

	static class DummyExecutor extends AbstractExecutorService {

		private boolean isShutdown=false;

		public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
			return false;
		}

		public boolean isShutdown() {
			return isShutdown;
		}

		public boolean isTerminated() {
			return isShutdown;
		}

		public void shutdown() {
			isShutdown=true;
		}

		public List<Runnable> shutdownNow() {
			return Collections.emptyList();
		}

		public void execute(Runnable command) {
			assert !isShutdown : "I'm already shutdown";
			command.run();
		}
		
	}

	private static class TransformingCollection<F,T>
		extends AbstractCollection<Callable<T>> {

		final Collection<? extends F> backingCollection;
		final Transformer<F, T> transformer;

		public TransformingCollection(Transformer<F,T> trans,
				Collection<? extends F> in) {
			transformer=trans;
			backingCollection=in;
		}

		@Override
		public Iterator<Callable<T>> iterator() {
			return new Titerator();
		}

		@Override
		public int size() {
			return backingCollection.size();
		}

		class Titerator implements Iterator<Callable<T>> {

			final Iterator<? extends F> backingIterator=
				backingCollection.iterator();

			public boolean hasNext() {
				return backingIterator.hasNext();
			}

			public Callable<T> next() {
				return new Callable<T>() {
					public T call() throws Exception {
						return transformer.transform(backingIterator.next());
					}
				};
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		}
	}
}
