// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 78F2256E-1110-11D9-9887-000A957659CC

package net.spy.net;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import net.spy.SpyObject;

/**
 * URLWatcher watches URLs and provides access to the most recent data from
 * the URL.
 */
public class URLWatcher extends SpyObject {

	private static URLWatcher instance=null;

	private Timer timer=null;
	private Map<URL, URLItem> items=null;

	// This lets it know when to give up
	private int recentTouches=0;

	/**
	 * Get an instance of URLWatcher.
	 */
	protected URLWatcher() {
		super();
		timer=new Timer("URLWatcher", true);
		items=new ConcurrentHashMap<URL, URLItem>();
	}

	/** 
	 * Get the static instance of URLWatcher.
	 * 
	 * @return the URLWatcher
	 */
	public static synchronized URLWatcher getInstance() {
		if(instance==null || instance.timer == null) {
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
	public String toString() {
		int numPages=items.size();
		return(super.toString() + " - " + numPages + " pages monitored");
	}

	// Get the URLItem for the given URL.
	@SuppressWarnings("unchecked")
	private URLItem getURLItem(URL u) {
		return(items.get(u));
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
	 * @param u The item to watch
	 */
	@SuppressWarnings("unchecked")
	public void startWatching(URLItem u) {
		if(!isWatching(u.getURL())) {
			items.put(u.getURL(), u);
			timer.scheduleAtFixedRate(u, 0, u.getUpdateFrequency());
		}
		// After adding it, wait a bit to see if it can grab the content
		synchronized(u) {
			try {
				u.wait(5000);
			} catch(InterruptedException e) {
				getLogger().info("Someone interrupted my sleep", e);
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
			if(timer != null) {
				timer.cancel();
				timer=null;
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

}
