// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 78F2256E-1110-11D9-9887-000A957659CC

package net.spy.net;

import java.util.Iterator;

import java.net.URL;

import java.io.IOException;

import net.spy.cron.JobQueue;
import net.spy.cron.Cron;

import net.spy.SpyObject;

/**
 * URLWatcher watches URLs and provides access to the most recent data from
 * the URL.
 */
public final class URLWatcher extends SpyObject {

	private static URLWatcher instance=null;

	private Cron cron=null;

	// How long to sleep between updates
	private static final int NAP_TIME=60000;

	private int numRuns=0;

	// This lets it know when to give up
	private int recentTouches=0;

	/**
	 * Get an instance of URLWatcher.
	 */
	private URLWatcher() {
		super();
		JobQueue jq=new JobQueue();
		cron=new Cron("URLWatcher Cron", jq);
	}

	/** 
	 * Get the static instance of URLWatcher.
	 * 
	 * @return the URLWatcher
	 */
	public static synchronized URLWatcher getInstance() {
		if(instance==null || instance.cron == null
			|| (!instance.cron.isRunning())
			|| (!instance.cron.isAlive())) {
			instance=new URLWatcher();
		}
		return(instance);
	}

	/** 
	 * String me.
	 */
	public String toString() {
		int numPages=cron.getJobQueue().size();
		return(super.toString() + " - " + numPages + " pages monitored, "
			+ numRuns + " runs");
	}

	// Get the URLItem for the given URL.
	private URLItem getURLItem(URL u) {
		URLItem ui=null;

		JobQueue jq=cron.getJobQueue();
		synchronized(jq) {
			// Look at each item for the match
			for(Iterator i=jq.iterator(); ui==null && i.hasNext();) {
				URLItem tmp=(URLItem)i.next();

				if(tmp.getURL().equals(u)) {
					ui=tmp;
				} // It's a match
			} // All items
		} // lock

		return(ui);
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
	public void startWatching(URLItem u) {
		JobQueue jq=cron.getJobQueue();
		synchronized(jq) {
			// Don't add it if it's already there
			if(!isWatching(u.getURL())) {
				jq.addJob(u);
			}
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
			if(cron.isRunning()) {
				cron.shutdown();
			}
		}
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
			ui=new URLItem(u);
			startWatching(ui);
		}
		// Return the current content.
		return(ui.getContent());
	}

}
