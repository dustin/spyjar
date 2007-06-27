package net.spy.pool;

import net.spy.SpyObject;

/**
 * Perform garbage collection with rate control.
 */
public final class GarbageCollector extends SpyObject {

	private static GarbageCollector gcInstance=null;

	private long lastRun=0;
	// Minimum time between calls.
	private static final int MIN_SLEEP=5000;

	private boolean inProgress=false;

	private GarbageCollector() {
		super();
	}

	/**
	 * Get the collector.
	 */
	public static synchronized GarbageCollector getGarbageCollector() {
		if(gcInstance==null) {
			gcInstance=new GarbageCollector();
		}
		return(gcInstance);
	}

	/**
	 * Run the garbage collection and perform finalization.
	 */
	public synchronized void collect() {
		long now=System.currentTimeMillis();

		if((!inProgress) && (now - lastRun) > MIN_SLEEP) {
			inProgress=true;
			try {
				getLogger().debug("Running gc and finalization");
				System.gc();
				System.runFinalization();
			} finally {
				// Make sure we mark us as not being in progress
				inProgress=false;
			}
			lastRun=now;
		} else {
			if(getLogger().isDebugEnabled()) {
				GCWarning gw=new GCWarning("Too soon for a garbage collection");
				getLogger().debug(gw);
			}
		}
	}

	private static class GCWarning extends Exception {
		public GCWarning(String msg) {
			super(msg);
		}
	}
}
