/*
 * Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
 *
 * $Id: SpyConfig.java,v 1.6 2003/08/05 09:01:02 dustin Exp $
 */

package net.spy;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;

import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;

/**
 * SpyConfig - an abstracted config file maintainer.
 *
 * The current config file format is that of a java Properties file.  This
 * class makes it easier to load them, caches them, and gives a good base
 * for extensions that load default config files for projects that are hard
 * to pass config filepaths into.
 */

public class SpyConfig extends Properties {
	private static Map configStore=null;

	/**
	 * Construct a new SpyConfig object describing a config file.
	 *
	 * @param conffile The config file we are describing.
	 */
	public SpyConfig(File conffile) {
		super();
		confInit();
		loadConfig(conffile);
	}

	/**
	 * Construct a new SpyConfig object describing a config file.
	 *
	 * @deprecated Give it a File object instead.
	 */
	public SpyConfig(String conffile) {
		this(new File(conffile));
	}

	/**
	 * Construct a new SpyConfig object without a config file.
	 */
	public SpyConfig() {
		super();

		confInit();
	}

	private static synchronized void confInit() {
		if(configStore==null) {
			configStore=Collections.synchronizedMap(new HashMap());
		}
	}

	/**
	 * Try to load a config file.  This function exists primarily for
	 * classes that extend SpyConfig and want to have multiple default
	 * locations for config files.
	 *
	 * @param conffile Path to the configuration file to load.
	 *
	 * @return true if the config loaded successfully.
	 */
	public boolean loadConfig(File conffile) {
		boolean loaded=false;

		// See whether we've cached the config file or not.
		if(isUpToDate(conffile)) {
			// We've already generated this, set it here.
			ConfigInfo ci=(ConfigInfo)configStore.get(conffile);
			set(ci.getConfig());
			loaded=true;
		} else {
			try {
				Map h = mapConfig(conffile);
				record(conffile, h);
				set(h);
				loaded=true;
			} catch(IOException e) {
				// Didn't load, this is not considered a failure.
			}
		}

		return(loaded);
	}

	/**
	 * Try to load a config file.  This function allows an object to load a
	 * config file from a list of files.  Only the first file in the list
	 * that works is actually loaded.
	 *
	 * @param confFiles an array of config file paths to attempt to load
	 *
	 * @return true if a config file was loaded
	 *
	 * @deprecated Use the File one instead.
	 */
	public boolean loadConfig(String confFiles[]) {
		boolean gotit=false;
		for(int i=0; i<confFiles.length && gotit==false; i++) {
			gotit=loadConfig(new File(confFiles[i]));
		}
		return(gotit);
	}

	/**
	 * Try to load a config file.  This function allows an object to load a
	 * config file from a list of files.  Only the first file in the list
	 * that works is actually loaded.
	 *
	 * @param confFiles an array of config file paths to attempt to load
	 *
	 * @return true if a config file was loaded
	 */
	public boolean loadConfig(File confFiles[]) {
		boolean gotit=false;
		for(int i=0; i<confFiles.length && gotit==false; i++) {
			gotit=loadConfig(confFiles[i]);
		}
		return(gotit);
	}

	// Check to see if we have current data on this file.
	private boolean isUpToDate(File file) {
		boolean r = false;
		ConfigInfo ci=(ConfigInfo)configStore.get(file);
		if(ci!=null) {
			if(ci.getTimestamp() == file.lastModified()) {
				r=true;
			}
		}
		return(r);
	}

	// record stuff to keep up with config file status
	private void record(File file, Map h) {
		configStore.put(file, new ConfigInfo(h, file.lastModified()));
	}

	private void set(Map h) {
		for(Iterator i=h.entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			put(me.getKey(), me.getValue());
		}
	}

	/**
	 * Get the value for a given config entry.
	 *
	 * @param key which config entry to return.
	 */
	public String get(String key) {
		return((String)super.get(key));
	}

	/**
	 * Get the value for a given config entry, with default.
	 *
	 * @param key which config entry to return.
	 * @param def default in case the entry doesn't exist.
	 */
	public String get(String key, String def) {
		String ret=get(key);
		if(ret==null) {
			ret=def;
		}
		return(ret);
	}

	/**
	 * Get an int value for a given config entry.  Please note, a default
	 * is *required* because undefined ints suck.
	 *
	 * @param key which config entry to return.
	 * @param def default in case the entry doesn't exist.
	 */
	public int getInt(String key, int def) {
		String value=get(key);
		int r=def;
		if(value!=null) {
			r=Integer.parseInt(value);
		}
		return(r);
	}

	/**
	 * Assign a value to the config only if it doesn't already exist.
	 * Useful for setting defaults.
	 *
	 * @param key config key
	 * @param value config value
	 *
	 */
	public void orput(String key, String value) {
		if(!containsKey(key)) {
			put(key, value);
		}
	}

	private Map mapConfig(File file) throws IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(file));
		return(p);
	}

	// Inner class for storing configuration information
	private static class ConfigInfo extends Object {

		private long timestamp=0;
		private Map config=null;

		public ConfigInfo(Map m, long ts) {
			super();
			this.config=m;
			this.timestamp=ts;
		}


		public long getTimestamp() {
			return(timestamp);
		}

		public Map getConfig() {
			return(config);
		}
	}
}
