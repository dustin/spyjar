/*
 * Copyright (c) 2000 Dustin Sallings <dustin@spy.net>
 *
 * $Id: SpyLogQueue.java,v 1.3 2003/08/01 07:16:53 dustin Exp $
 */

package net.spy.log;

import java.util.Hashtable;
import java.util.Vector;

import net.spy.SpyObject;

/**
 * This class performs the actual queue management for the SpyLog system.
 * It should probably not be referenced directly.
 */
public class SpyLogQueue extends SpyObject {
	private static Hashtable queues=null;
	private String queueName=null;

	/**
	 * Get a new SpyLogQueue with the given name.
	 */
	public SpyLogQueue(String name) {
		super();
		this.queueName=name;
		init();
	}

	private synchronized void init() {
		if(queues==null) {
			queues=new Hashtable();
		}

		Vector v=(Vector)queues.get(queueName);
		if(v==null) {
			v=new Vector();
			queues.put(queueName, v);
		}
	}

	private synchronized Vector getQueue() {
		Vector v=(Vector)queues.get(queueName);
		return(v);
	}

	/**
	 * Add a new item to a queue.
	 *
	 * @param e item to be added
	 */
	public synchronized void addToQueue(SpyLogEntry e) {
		Vector v=getQueue();
		synchronized(v) {
			v.addElement(e);
			v.notify();
		}
		notify();
	}

	/**
	 * Wait for notification of an addition in the queue.
	 *
	 * @param ms The maximum number of milliseconds to wait.
	 */
	public void waitForQueue(long ms) {
		if(size()<=0) {
			// Only do this if we don't already think we have data
			try {
				synchronized(this) {
					wait(ms);
				}
			} catch(InterruptedException e) {
				// If we are going to return too early, pause just a sec
				getLogger().warn("SpyLogQueue.waitForQueue got exception", e);
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e2) {
					getLogger().warn("Wow, this sucks, got another one", e2);
				}
			}
		} // if we have no data
	}

	/**
	 * Return the current size of the queue.  This may change before you
	 * can do anything about it, so use it wisely.
	 */
	public synchronized int size() {
		Vector logBuffer=getQueue();
		int size=logBuffer.size();
		return(size);
	}

	/**
	 * Flush the current log entries -- DO NOT CALL THIS.  This is for
	 * SpyLogFlushers only.
	 */
	public synchronized Vector flush() {
		Vector ret=null;
		Vector logBuffer=getQueue();
		ret=logBuffer;          // Copy the old vector's reference.
		logBuffer=new Vector(); // Create a new one.
		queues.put(queueName, logBuffer);
		return(ret);
	}
}
