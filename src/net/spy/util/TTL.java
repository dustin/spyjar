// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 8B221C1B-1110-11D9-8B85-000A957659CC

package net.spy.util;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.text.MessageFormat;

import net.spy.SpyObject;

/**
 * A TTL object is used to express an intent for a process to finish within
 * a certain amount of time.
 *
 * <p>
 *  A TTL object must be registered with a {@link TTLMonitor} before it
 *  will report.  The default implementation of report logs via
 *  {@link Logger}.
 * </p>
 */
public class TTL extends SpyObject {

	private static final int DEFAULT_REPORT_INTERVAL=300000;
	private static final int DEFAULT_N_REPORTS=10;

	private long ttl=0;
	private long startTime=0;
	private Expired e=null;
	private Object extraInfo=null;

	private boolean isClosed=false;
	private long lastReport=0;
	private int nReports=0;

	private int reportInterval=DEFAULT_REPORT_INTERVAL;
	private int maxNReports=DEFAULT_N_REPORTS;

	/**
	 * Get an instance of TTL.
	 * @param ttl Number of milliseconds until the TTL fires
	 */
	public TTL(long ttl) {
		this(ttl, null);
	}

	/** 
	 * Get an instance of TTL with the given ttl and extra object.
	 * @param ttl Number of milliseconds until the TTL fires
	 * @param extraInfo Extra info that will be toString()ed in the log
	 */
	public TTL(long ttl, Object extraInfo) {
		this.ttl=ttl;
		this.extraInfo=extraInfo;
		this.reset();
		this.e=new Expired();
	}

	/** 
	 *  Resets the counter by setting the time that the TTL started to
	 *  <i>right now</i>.
	 */
	public void reset() {
		this.startTime=System.currentTimeMillis();
	}

	/**
	 * String me.
	 */
	public String toString() {
		return("TTL:  " + ttl);
	}

	/** 
	 * Get the number of milliseconds this TTL object is expected to be in
	 * use.
	 */
	public long getTTL() {
		return(ttl);
	}

	/** 
	 * Set the minimum interval at which doReport() should be called when
	 * TTLMonitor sees this object as expired.
	 */
	public void setReportInterval(int ms) {
		this.reportInterval=ms;
	}

	/** 
	 * Set the maximum number of reports this TTL should issue before
	 * automatically closing.
	 */
	public void setMaxReports(int to) {
		maxNReports=to;
	}

	/** 
	 * True if the TTL object has reported.
	 */
	public boolean hasReported() {
		return(nReports>0);
	}

	/**
	 * Provide extra information for the TTL report.
	 */
	public void setExtraInfo(Object o) {
		this.extraInfo=o;
	}

	/**
	 * Get the extra info provided for the TTL report.
	 */
	public Object getExtraInfo() {
		return(extraInfo);
	}

	/**
	 * Calling this method states that we are no longer interested in the
	 * progress of this TTL.
	 */
	public void close() {
		isClosed=true;
	}

	/**
	 * Return true if this TTL is no longer interesting.
	 */
	public boolean isClosed() {
		return(isClosed);
	}

	/** 
	 * Ask a TTL if it's expired.
	 * 
	 * @return true if the TTL is expired
	 */
	public boolean isExpired() {
		boolean rv=false;
		long now=System.currentTimeMillis();

		if(!isClosed()) {
			if(now > startTime+ttl) {
				rv=true;
			}
		}

		return(rv);
	}

	// Has it been long enough since our last report?
	private boolean readyForReport() {
		long now=System.currentTimeMillis();

		return((now-lastReport) > reportInterval);
	}

	/** 
	 * Report a TTL expiration with the given format.
	 *
	 * <ul>
	 *  <li> 0 - Time the TTL object has been open.</li>
	 *  <li> 1 - Time the TTL was expected to be running.</li>
	 *  <li> 2 - Extra info passed in (may be null).</li>
	 * </ul>
	 * 
	 * @param msg message format string to print when the TTL expires
	 */
	protected void reportWithFormat(String msg) {
		long now=System.currentTimeMillis();
		Object args[]={new Long(now-startTime), new Long(ttl), extraInfo};

		MessageFormat mf=new MessageFormat(msg);
		String toLog=mf.format(args);

		getLogger().warn(toLog, e);
	}

	/** 
	 * Get the message format string from the named bundle.
	 * 
	 * @param bundleName the name of the bundle from which to get the messages
	 * @param msgNoArg the key in the bundle to use when there's no extra info
	 * @param msgWithArg the key in the bundle to use when there's extra info
	 *
	 * @return the format string
	 */
	protected String getMessageFromBundle(String bundleName,
		String msgNoArg, String msgWithArg) {

		String rv=null;

		ResourceBundle rb=null;
		try {
			rb=ResourceBundle.getBundle(bundleName);
		} catch(MissingResourceException e) {
			rv="ResourceBundle not found while reporting TTL expiration:  "
				+ bundleName + ".  (Expected {1}ms, been {0}ms).";
		}

		if(rb!=null) {
			try {
				if(extraInfo==null) {
					rv=rb.getString(msgNoArg);
				} else {
					rv=rb.getString(msgWithArg);
				}
			} catch(MissingResourceException e) {
				rv="ResourceBundle not found while reporting TTL expiration:  "
					+ e.getKey() + ".  (Expected {1}ms, been {0}ms).";
			}
		}

		return (rv);
	}

	/** 
	 * Called when an object's TTL has expired without closing.
	 */
	protected void doReport() {

		// Get the message.
		String msg=getMessageFromBundle("net.spy.util.messages",
			"ttl.msg", "ttl.msg.witharg");

		reportWithFormat(msg);
	}

	/**
	 * Request a report of the TTL.
	 *
	 * This is called whenever the TTLMonitor owning this object notices
	 * the object's TTL has expired.
	 */
	public final void report() {
		if(isExpired() && readyForReport()) {
			long now=System.currentTimeMillis();
			lastReport=now;

			// Call the actual report routine
			doReport();

			// If we've reported enough times, give up.
			if(nReports++ >= maxNReports) {
				close();
			}
		}
	}

	// This does not extend Exception because it exists primarily for the
	// message it prints.
	private static class Expired extends Exception {
		public Expired() {
			super("Timer has expired.");
		}
	}

}
