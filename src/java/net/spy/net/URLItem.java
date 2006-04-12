// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 78AC4743-1110-11D9-9A79-000A957659CC

package net.spy.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * A particular URL that's being watched.
 */
public class URLItem extends TimerTask {

	private static final int DEFAULT_UPDATE_FREQ = 900000;
	// How long a URL will be watched if nobody wants it (defaults to a
	// half hour).
	private int maxIdleTime=1800000;
	private int updateFrequency=DEFAULT_UPDATE_FREQ;

	private long lastRequest=0;

	private int numUpdates=0;

	private URL url=null;

	private Map<String, List<String>> lastHeaders=null;
	private String content=null;
	private long lastModified=0;
	private boolean isRunning=true;

	private IOException lastError=null;

	private Logger logger=null;

	/**
	 * Get an instance of URLItem.
	 *
	 * @param u URL to watch
	 */
	public URLItem(URL u) {
		this(u, DEFAULT_UPDATE_FREQ);
	}

	/**
	 * Get an instance of URLItem.
	 *
	 * @param u URL to watch
	 * @param i the update frequency
	 */
	public URLItem(URL u, int i) {
		super();
		this.updateFrequency=i;
		this.url=u;
		lastRequest=System.currentTimeMillis();
		logger=LoggerFactory.getLogger(getClass());
	}

	/**
	 * Get a fetcher (override for testing).
	 */
	protected HTTPFetch getFetcher(Map<String, List<String>> headers) {
		return(new HTTPFetch(url, headers));
	}

	private synchronized void setContent(String to,
		Map<String, List<String>> headers, long lastMod) {
		content=to;
		// Big chunk of debug logging.
		if(logger.isDebugEnabled()) {
			logger.debug("Setting content for %s: %s", this,
					content==null?"<null>":content.length() + " bytes");
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
		if(logger.isDebugEnabled()) {
			logger.debug("Getting content for %s:  %s", this,
					content==null?"<null>":content.length() + " bytes");
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

	/**
	 * True if this is still running.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Get the update frequency.
	 */
	public int getUpdateFrequency() {
		return updateFrequency;
	}

	public void run() {
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
			HTTPFetch hf=getFetcher(headers);
			hf.setIfModifiedSince(lastModified);

			if(hf.getStatus() == HttpURLConnection.HTTP_OK) {
				setContent(hf.getData(), hf.getResponseHeaders(),
					hf.getLastModified());
			} else {
				logger.info("Not saving content due to response status %s",
					hf.getStatus());
			}
		} catch(IOException e) {
			lastError=e;
		}
		if((System.currentTimeMillis() - lastRequest) > maxIdleTime) {
			cancel();
			isRunning=false;
		}
	}

}
