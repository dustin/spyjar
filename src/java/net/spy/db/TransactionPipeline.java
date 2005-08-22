// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
// arch-tag: 77725A84-15D7-11D9-8432-000A957659CC

package net.spy.db;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;
import net.spy.util.ThreadPool;

/**
 * Asynchronous Saver.
 */
public class TransactionPipeline extends SpyObject {

	// Thread pool name
	private static final String POOL_NAME="TransactionPipeline Workers ";
	// Default size of the transaction pipeline pool
	private static final int DEFAULT_POOL_SIZE=1;

	// Minimum amount of time (in milliseconds) a transaction has to have been
	// in the pipeline before it will be considered for processing
	private static final int MIN_TRANS_AGE=500;
	// Maximum amount of time we'll sleep waiting for transactions
	private static final int MAX_SLEEP_TIME=MIN_TRANS_AGE*3;

	// The thread pool.
	private ThreadPool pool=null;

	/**
	 * Get an instance of TransactionPipeline.
	 */
	public TransactionPipeline() {
		super();
		pool=new ThreadPool(POOL_NAME, DEFAULT_POOL_SIZE);
	}

	/** 
	 * Shut down the pipeline.
	 */
	public synchronized void shutdown() {
		if(pool == null) {
			throw new IllegalStateException("Already shut down!");
		}
		try {
			// Let the pool shut down and all that.
			pool.waitForCompletion();
		} catch(InterruptedException e) {
			getLogger().warn("Interrupted waiting for pool completion", e);
		}
		// Mark it as null so we won't do it again
		pool=null;
	}

	/** 
	 * Add a transaction to the pipeline.
	 * 
	 * @param s the savable
	 * @param conf the configuration
	 * @param context a context for the save
	 */
	public void addTransaction(Savable s, SpyConfig conf, SaveContext context) {
		pool.addTask(new PipelineTask(s, conf, context));
	}

	/** 
	 * Add a transaction to the pipeline without a context.
	 * 
	 * @param s the savable
	 * @param conf the configuration
	 */
	public void addTransaction(Savable s, SpyConfig conf) {
		addTransaction(s, conf, null);
	}

	private static final class PipelineTask extends SpyObject
		implements Runnable {

		// Throwable filled in with the stack holding the context of where the
		// pipeline request was original requested
		private Throwable originalStack=null;

		private Saver saver=null;
		private Savable toSave=null;

		private long startDate=0;

		private PipelineTask(Savable s, SpyConfig conf, SaveContext context) {
			super();
			this.originalStack=new Exception("Original request");
			originalStack.fillInStackTrace();

			this.toSave=s;
			this.saver=new Saver(conf, context);

			startDate=System.currentTimeMillis();
		}

		/** 
		 * Run the transaction.
		 */
		public void run() {
			try {
				// How long should we sleep before processing?
				long age=System.currentTimeMillis() - startDate;
				int sleepTime=(int)(MIN_TRANS_AGE - age);
				if(sleepTime > MAX_SLEEP_TIME) {
					sleepTime=MAX_SLEEP_TIME;
				}
				if(sleepTime > 0) {
					if(getLogger().isDebugEnabled()) {
						getLogger().debug("Sleeping for " + sleepTime + "ms");
					}
					Thread.sleep(sleepTime);
				}
				saver.save(toSave);
			} catch(Throwable t) {
				getLogger().error("Error saving asynchronous transaction", t);
				getLogger().error("Sent from", originalStack);
			}
		} // run()

	} // class PipelineTask

} // class TransactionPipeline
