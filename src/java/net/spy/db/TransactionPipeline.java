// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
// arch-tag: 77725A84-15D7-11D9-8432-000A957659CC

package net.spy.db;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;

/**
 * Asynchronous Saver.
 */
public class TransactionPipeline extends SpyObject {

	// Thread pool name
	private static final String POOL_NAME="TransactionPipeline Worker";
	// Default size of the transaction pipeline pool
	private static final int DEFAULT_POOL_SIZE=1;

	// Minimum amount of time (in milliseconds) a transaction has to have been
	// in the pipeline before it will be considered for processing
	private static final int MIN_TRANS_AGE=500;

	// The thread pool.
	private ScheduledThreadPoolExecutor pool=null;

	/**
	 * Get an instance of TransactionPipeline.
	 *
	 * @param tg the thread group under which threads will be created
	 * @param name an optional suffix to the names of the worker created
	 */
	public TransactionPipeline(final ThreadGroup tg, String name) {
		super();
		final String n=POOL_NAME+(name==null?"": " " + name);
		pool=new ScheduledThreadPoolExecutor(DEFAULT_POOL_SIZE,
				new ThreadFactory() {
					public Thread newThread(Runnable r) {
						Thread rv=new Thread(tg, r, n);
						return rv;
					}
		});
	}

	/** 
	 * Get a transaction pipeline with no specific name.
	 */
	public TransactionPipeline() {
		this(null, null);
	}

	/** 
	 * Shut down the pipeline.
	 */
	public synchronized void shutdown() {
		assert pool != null : "Trying to double-shutdown a pool.";
		pool.shutdown();
		pool = null;
	}

	/** 
	 * Add a transaction to the pipeline.
	 * 
	 * @param s the savable
	 * @param conf the configuration
	 * @param context a context for the save
	 */
	public ScheduledFuture<?> addTransaction(
			Savable s, SpyConfig conf, SaveContext ctx) {
		return pool.schedule(new PipelineTask(s, conf, ctx), MIN_TRANS_AGE,
				TimeUnit.MILLISECONDS);
	}

	/** 
	 * Add a transaction to the pipeline without a context.
	 * 
	 * @param s the savable
	 * @param conf the configuration
	 */
	public ScheduledFuture<?> addTransaction(Savable s, SpyConfig conf) {
		return addTransaction(s, conf, null);
	}

	private static final class PipelineTask extends SpyObject
		implements Runnable {

		// Throwable filled in with the stack holding the context of where the
		// pipeline request was original requested
		private Throwable originalStack=null;

		private Savable toSave=null;
		private SpyConfig conf=null;
		private SaveContext context=null;

		private PipelineTask(Savable s, SpyConfig cnf, SaveContext ctx) {
			super();
			this.originalStack=new Exception("Original request");
			originalStack.fillInStackTrace();

			this.toSave=s;
			conf=cnf;
			context=ctx;
		}

		/** 
		 * Run the transaction.
		 */
		public void run() {
			try {
				new Saver(conf, context).save(toSave);
			} catch(Throwable t) {
				getLogger().error("Error saving asynchronous transaction", t);
				getLogger().error("Sent from", originalStack);
			}
		} // run()

	} // class PipelineTask

} // class TransactionPipeline
