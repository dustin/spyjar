// Copyright (c) 2001  Beyond.com <dustin@beyond.com>
//
// arch-tag: 81BE7382-1110-11D9-8F47-000A957659CC

package net.spy.util;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.util.Date;
import java.util.Hashtable;

import net.spy.SpyObject;

/**
 * Generic Debug logger.  Logs to a file that's listed in a System
 * property, or does not log if the System property is not set.
 *
 * @deprecated use {@link net.spy.log.Logger} instead
 */
public class Debug extends SpyObject {

	// Storage for all the file thingies
	private static Hashtable debugs=null;
	private String propname=null;

	/**
	 * Get a new Debug object that writes its output to the Fle specified
	 * in the given System property.
	 */
	public Debug(String propname) {
		super();
		this.propname=propname;
		init();
	}

	private void init() {
		synchronized(Debug.class) {
			if(debugs==null) {
				debugs=new Hashtable();
			}

			// If we don't have the debugstream, try to get it.
			if(debugs.get(propname)==null) {

				String of=System.getProperty(propname);

				if(of!=null) {
					try {
						FileOutputStream fos=new FileOutputStream(of, true);
						PrintWriter debugOut=new PrintWriter(fos);
						debugOut.println(getTimestamp() + ":  Initialized.");
						debugs.put(propname, debugOut);
					} catch(Exception e) {
						getLogger().warn("Cannot initialize debugging", e);
						e.printStackTrace();
					}
				} // Got a filename
			} // get a prop name
		} // synchronized()
	} // init()

	// Get a timestamp for logging
	private static String getTimestamp() {
		return("[" + new Date().toString() + "]");
	}

	/**
	 * Log a debug message.
	 */
	public void debug(String msg) {
		PrintWriter debugOut=(PrintWriter)debugs.get(propname);
		if(debugOut!=null) {
			String tmsg=getTimestamp() + " " + msg;
			synchronized(debugOut) {
				debugOut.println(tmsg);
				debugOut.flush();
			}
		}
	}

}
