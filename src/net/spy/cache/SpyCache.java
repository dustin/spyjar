/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * arch-tag: 616FA28A-1110-11D9-99F2-000A957659CC
 */

package net.spy.cache;

import java.lang.ref.Reference;

import java.io.IOException;

import java.net.InetAddress;

import java.util.Map;
import java.util.Iterator;

import net.spy.SpyObject;
import net.spy.SpyThread;
import net.spy.util.TimeStampedHashMap;

/**
 * Spy in-memory cache object.
 *
 * <p>
 *
 * If the system properties <tt>net.spy.cache.multi.addr</tt> and
 * <tt>net.spy.cache.multi.port</tt> are both set, requests may be sent as
 * ASCII strings on that multicast group and port to clear cache entries
 * based on prefix.
 */
public class SpyCache extends SpyObject {

	private TimeStampedHashMap cacheStore=null;
	private SpyCacheCleaner cacheCleaner=null;

	private static SpyCache instance=null;

	// how frequently to clean up the cache
	private static final int CACHE_CLEAN_SLEEP_TIME=60000;

	/**
	 * Construct a new instance of SpyCache.  This allows subclasses to
	 * override certain methods, allowing smarter cache handling.
	 */
	protected SpyCache() {
		super();

		init();
	}

	private void init() {
		cacheStore=new TimeStampedHashMap();
	}

	private synchronized void checkThread() {
		if(cacheCleaner==null || (!cacheCleaner.isAlive())) {
			cacheCleaner=new SpyCacheCleaner();
		}
	}

	/**
	 * Get the instance of SpyCache.
	 *
	 * @return the instance of SpyCache, or a new instance if required
	 */
	public static synchronized SpyCache getInstance() {
		if(instance==null) {
			instance=new SpyCache();
		}

		instance.checkThread();

		return(instance);
	}

	/** 
	 * Store a Cachable object in the cache.
	 * 
	 * @param key the key for storing this object
	 * @param value the object to store
	 */
	public void store(String key, Cachable value) {
		synchronized(cacheStore) {
			// Send the cached event notify to the cachable itself
			value.cachedEvent(key);
			cacheStore.put(key, value);
		}
	}

	/**
	 * Store an object in the cache with the specified timeout.
	 *
	 * <p>
	 *  Objects will be wrapped in a private subclass of {@link Cachable}
	 *  that expires based on the time.
	 * </p>
	 *
	 * @param key Cache key
	 * @param value Object to cache
	 * @param cacheTime Amount of time (in milliseconds) to store object.
	 */
	public void store(String key, Object value, long cacheTime) {
		Cachable i=new SpyCacheItem(key, value, cacheTime);
		store(key, i);
	}

	/**
	 * Get an object from the cache, returns null if there's not a valid
	 * object in the cache with this key.
	 *
	 * @param key key of the object to return
	 * @return the object, else null
	 */
	public Object get(String key) {
		Object ret=null;
		long t=System.currentTimeMillis();
		synchronized(cacheStore) {
			Cachable i=(Cachable)cacheStore.get(key);
			if(i!=null && (!i.isExpired())) {
				// mark the object as seen
				i.setAccessTime(t);
				// get the object from the cache
				ret=i.getCachedObject();
				// If the stored object is a reference, dereference it.
				if((ret!=null) && (ret instanceof Reference)) {
					Reference ref=(Reference)ret;
					ret=ref.get();
				} // Object was a reference
			} // Found object in cache
		} // Locked the cache store
		return(ret);
	}

	/**
	 * Manually remove an object from the cache.
	 *
	 * @param key key to remove
	 */
	public void uncache(String key) {
		synchronized(cacheStore) {
			Cachable unc=(Cachable)cacheStore.remove(key);
			if(unc!=null) {
				unc.uncachedEvent(key);
			}
		}
	}

	/**
	 * Remove all objects from the cache that begin with the passed in
	 * string.
	 *
	 * @param keystart string to match in the key name
	 */
	public void uncacheLike(String keystart) {
		synchronized(cacheStore) {
			for(Iterator i=cacheStore.entrySet().iterator(); i.hasNext();) {
				Map.Entry me=(Map.Entry)i.next();

				String key=(String)me.getKey();

				// If this matches, kill it.
				if(key.startsWith(keystart)) {
					i.remove();
					Cachable c=(Cachable)me.getValue();
					c.uncachedEvent(key);
				}
			} // for loop
		} // lock
	}

	////////////////////////////////////////////////////////////////////
	//                       Private Classes                          //
	////////////////////////////////////////////////////////////////////

	private class SpyCacheCleaner extends SpyThread {

		// How many cleaning passes we've done.
		private int passes=0;

		// This is so we can only report multicast security exceptions
		// once.
		private boolean reportedMulticastSE=false;

		// Insert multicast listener here.
		private CacheClearRequestListener listener=null;

		// This indicates whether we need to go through the multicast cache
		// clearer loop.  It's true by default so we'll try it the first time.
		private boolean wantMulticastListener=true;

		public SpyCacheCleaner() {
			super();
			setName("SpyCacheCleaner");
			setDaemon(true);
			start();
		}

		public String toString() {
			return(super.toString() + " - "
				+ passes + " runs, mod age:  " + cacheStore.getUseAge()
				+ ", cur size:  " + cacheStore.size()
				+ ", tot stored:  " + cacheStore.getNumPuts()
				+ ", watermark:  " + cacheStore.getWatermark()
				+ ", hits:  " + cacheStore.getHits()
				+ ", misses:  " + cacheStore.getMisses()
				);
		}

		private void cleanup() throws Exception {
			long now=System.currentTimeMillis();
			synchronized(cacheStore) {
				for(Iterator i=cacheStore.values().iterator(); i.hasNext();){
					Cachable it=(Cachable)i.next();
					if(it.isExpired()) {
						if(getLogger().isDebugEnabled()) {
							getLogger().debug(it.getCacheKey() + " expired");
						}
						i.remove();
					}
				}
			}
			passes++;
		}

		private boolean shouldIContinue() {
			boolean rv=false;

			// Return true if the difference between now and the last
			// time the cache was touched is less than an hour.
			if((cacheStore.getUseAge()) < (3600*1000)) {
				rv=true;
			}

			return(rv);
		}

		// Make sure our multicast listener is still running if it should be.
		private void checkMulticastThread() {
			try {
				String addrS=System.getProperty("net.spy.cache.multi.addr");
				String portS=System.getProperty("net.spy.cache.multi.port");

				if(addrS!=null && portS!=null) {
					wantMulticastListener=true;
					int port=Integer.parseInt(portS);

					InetAddress group = InetAddress.getByName(addrS);
					listener=new CacheClearRequestListener(group, port);
				} else {
					wantMulticastListener=false;
				}

			} catch(SecurityException se) {
				// Only do this the first time.
				if(!reportedMulticastSE) {
					getLogger().error("Couldn't create multicast listener", se);
					reportedMulticastSE=true;
				}
			} catch(IOException ioe) {
				getLogger().error("Couldn't create multicast listener", ioe);
			}
		}

		/** 
		 * Loop until there's no need to loop any more.
		 */
		public void run() {

			boolean keepgoing=true;

			// It will keep going until nothing's been touched in the cache
			// for an hour, at which point it'll dump the whole cache and join.
			while(keepgoing) {
				try {
					// Just for throttling, sleep a second.
					sleep(CACHE_CLEAN_SLEEP_TIME);
					cleanup();
					// Check to see if we want a multicast listener
					if(wantMulticastListener
						&& (listener==null || (!listener.isAlive()))) {
						checkMulticastThread();
					}
				} catch(Exception e) {
					getLogger().warn("Exception in cleanup loop", e);
				}

				keepgoing=shouldIContinue();
			}

			getLogger().info("Shutting down.");

			// OK, we're about to bail, let's dump the cache and go.
			cacheStore.clear();

			// Tell the multicast listener to stop if we have one
			if(listener!=null) {
				listener.stopRunning();
			}
		}
	} // Cleaner class

	private static class SpyCacheItem extends AbstractCachable {
		private long exptime=0;

		public SpyCacheItem(Object key, Object value, long cacheTime) {
			super(key, value);

			exptime=getCacheTime()+cacheTime;
		}

		public String toString() {
			String out="Cached item:  " + getCacheKey();
			if(exptime>0) {
				out+="\nExpires:  " + new java.util.Date(exptime);
			}
			out+="\n";
			return(out);
		}

		public boolean isExpired() {
			boolean ret=false;
			if(exptime>0) {
				long t=System.currentTimeMillis();
				ret=(t>exptime);
			}
			// If the value is a reference that is no longer valid,
			// the object has expired
			Object v=getCachedObject();
			if(v instanceof Reference) {
				Reference rvalue=(Reference)v;
				if(rvalue.get()==null) {
					ret=false;
				}
			}
			return(ret);
		}

	} // SpyCacheItem

}
