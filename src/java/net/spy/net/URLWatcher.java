// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.net;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.SpyObject;

/**
 * URLWatcher watches URLs and provides access to the most recent data from
 * the URL.
 */
public class URLWatcher extends SpyObject {

	private static URLWatcher instance=null;

	private ScheduledExecutorService executor=null;
	private Map<URL, FutureURL> items=null;

	// This lets it know when to give up
	private int recentTouches=0;

	/**
	 * Get an instance of URLWatcher.
	 */
	protected URLWatcher() {
		super();
		executor=new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t=new Thread(r, "URLWatcher worker");
				t.setDaemon(true);
				return t;
			}
		});
		items=new ConcurrentHashMap<URL, FutureURL>();
	}

	/**
	 * Get the static instance of URLWatcher.
	 *
	 * @return the URLWatcher
	 */
	public static synchronized URLWatcher getInstance() {
		if(instance==null || instance.executor == null) {
			instance=new URLWatcher();
		}
		return(instance);
	}

	/**
	 * Set an instance (for testing).
	 */
	public static synchronized void setInstance(URLWatcher uw) {
		if(instance != null) {
			instance.shutdown();
		}
		instance=uw;
	}

	/**
	 * String me.
	 */
	@Override
	public String toString() {
		int numPages=items.size();
		return(super.toString() + " - " + numPages + " pages monitored");
	}

	// Get the URLItem for the given URL.
	private URLItem getURLItem(URL u) {
		FutureURL rv=items.get(u);
		if(rv != null && !rv.urlItem.isRunning()) {
			items.remove(u);
			rv.future.cancel(false);
			rv=null;
		}
		return(rv == null ? null : rv.urlItem);
	}

	/**
	 * Find out if this URLWatcher is watching a given URL.
	 *
	 * @param u the URL to test
	 * @return true if the URL is already being watched
	 */
	public boolean isWatching(URL u) {
		URLItem ui=getURLItem(u);
		return(ui != null);
	}

	/**
	 * Start watching the given URL.
	 *
	 * @param u The item to watch
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void startWatching(URLItem u) {
		if(!isWatching(u.getURL())) {
			ScheduledFuture<?> f=executor.scheduleAtFixedRate(u, 0,
					u.getUpdateFrequency(), TimeUnit.MILLISECONDS);
			items.put(u.getURL(), new FutureURL(f, u));
			try {
				u.waitForContent(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				getLogger().warn("Someone interrupted my sleep", e);
			} catch (TimeoutException e) {
				getLogger().warn("Timed out waiting for initial update");
			}
		}
	}

	/**
	 * Instruct the URLWatcher to stop URLWatching.
	 */
	public void shutdown() {
		synchronized(getClass()) {
			// Throw away the instance
			instance=null;
			// Shut down the cron
			if(executor != null) {
				List<Runnable> cancelled=executor.shutdownNow();
				if(cancelled.size() > 0) {
					getLogger().info("Shutting down cancelled %s", cancelled);
				}
				executor=null;
			}
		}
	}

	protected URLItem getNewURLItem(URL u) {
		return new URLItem(u);
	}

	/**
	 * Get the content (as a String) for a given URL.
	 *
	 * @param u The URL whose content we want
	 * @return The String contents, or null if none could be retreived
	 * @throws IOException if there was a problem updating the URL
	 */
	public String getContent(URL u) throws IOException {
		recentTouches++;
		URLItem ui=getURLItem(u);
		// If we don't have one for this URL yet, create it.
		if(ui==null) {
			ui=getNewURLItem(u);
			startWatching(ui);
		}
		// Return the current content.
		return(ui.getContent());
	}

	static class FutureURL {
		URLItem urlItem;
		ScheduledFuture<?> future;

		public FutureURL(ScheduledFuture<?> f, URLItem u) {
			super();
			future=f;
			urlItem=u;
		}
	}
}
