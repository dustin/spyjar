// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 78AC4743-1110-11D9-9A79-000A957659CC

package net.spy.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.spy.cron.Job;
import net.spy.cron.SimpleTimeIncrement;
import net.spy.cron.TimeIncrement;
import net.spy.util.ThreadPoolRunnable;

/**
 * A particular URL that's being watched.
 */
public class URLItem extends Job implements ThreadPoolRunnable {

	// How long a URL will be watched if nobody wants it (defaults to a
	// half hour).
	private int maxIdleTime=1800000;

	private long lastRequest=0;

	private int numUpdates=0;

	private URL url=null;

	private Map<String, List<String>> lastHeaders=null;
	private String content=null;
	private long lastModified=0;

	private IOException lastError=null;

	/** 
	 * Get a new URLItem at the default interval.
	 * 
	 * @param u URL to watch
	 */
	public URLItem(URL u) {
		this(u, new Date(), new SimpleTimeIncrement(1800000));
	}

	/** 
	 * Get a new URLItem with the given interval.
	 * 
	 * @param u URL to watch
	 * @param ti the increment
	 */
	public URLItem(URL u, TimeIncrement ti) {
		this(u, new Date(), ti);
	}

	/**
	 * Get an instance of URLItem.
	 *
	 * @param u URL to watch
	 * @param startDate time to start
	 * @param ti the increment
	 */
	public URLItem(URL u, Date startDate, TimeIncrement ti) {
		super(u.toString(), startDate, ti);
		this.url=u;
		lastRequest=System.currentTimeMillis();
	}

	/** 
	 * Ask the URL to update itself if it needs to.
	 */
	public void runJob() {
		HashMap<String, List<String>> headers=new HashMap<String, List<String>>();
		// make sure the stuff isn't cached
		ArrayList<String> tmp=new ArrayList<String>();
		tmp.add("no-cache");
		headers.put("Pragma", tmp);
		// But don't request something if we know we already have it.
		if(lastHeaders != null) {
			List<String> eTags=lastHeaders.get("ETag");
			if(eTags != null) {
				// Put the etags in the none-match
				headers.put("If-None-Match", eTags);
			}
		}

		numUpdates++;

		try {
			HTTPFetch hf=new HTTPFetch(url, headers);
			hf.setIfModifiedSince(lastModified);

			if(hf.getStatus() == HttpURLConnection.HTTP_OK) {
				setContent(hf.getData(), hf.getResponseHeaders(),
					hf.getLastModified());
			} else {
				getLogger().info("Not saving content due to response status "
					+ hf.getStatus());
			}
		} catch(IOException e) {
			lastError=e;
		}
	}

	private synchronized void setContent(String to,
		Map<String, List<String>> headers, long lastMod) {
		content=to;
		// Big chunk of debug logging.
		if(getLogger().isDebugEnabled()) {
			String contentDesc=null;
			if(to == null) {
				contentDesc="<null>";
			} else {
				contentDesc=to.length() + " bytes";
			}
			getLogger().debug("Setting content for " + this
				+ ":  " + contentDesc);
		}
		lastModified=lastMod;
		lastHeaders=headers;

		// Notify listeners that this has been updated.
		notifyAll();
	}

	/** 
	 * Get the content from the last fetch.
	 */
	public synchronized String getContent() throws IOException {
		lastRequest=System.currentTimeMillis();
		if(lastError!=null) {
			throw lastError;
		}
		if(getLogger().isDebugEnabled()) {
			String contentDesc=null;
			if(content == null) {
				contentDesc="<null>";
			} else {
				contentDesc=content.length() + " bytes";
			}
			getLogger().debug("Getting content for " + this + ":  "
				+ contentDesc);
		}
		return(content);
	}

	/** 
	 * Find out when the last request was.
	 * 
	 * @return the timestamp of the last request.
	 */
	public long getLastRequest() {
		return(lastRequest);
	}

	/** 
	 * Get the URL this thing is watching.
	 * 
	 * @return the URL
	 */
	public URL getURL() {
		return(url);
	}

	/** 
	 * Override the finished mark to also stop this job if it hasn't been
	 * touched recently enough.
	 */
	protected void markFinished() {
		long now=System.currentTimeMillis();
		// If it's been too long since this thing was touched, toss it.
		if( (now-lastRequest) > maxIdleTime) {
			stopRunning();
		}
		super.markFinished();
	}

	/** 
	 * Set the maximum number of milliseconds this URL will remain in the
	 * container if nothing requests it.
	 */
	public void setMaxIdleTime(int to) {
		this.maxIdleTime=to;
	}

	/** 
	 * Get the maximum number of milliseconds this URL will remain in the
	 * container if nothing requests it.
	 */
	public int getMaxIdleTime() {
		return(maxIdleTime);
	}

}
