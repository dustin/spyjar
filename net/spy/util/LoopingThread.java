// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: LoopingThread.java,v 1.2 2003/04/11 09:05:05 dustin Exp $

package net.spy.util;

import net.spy.SpyThread;

/**
 * A Thread that loops over a runLoop().
 */
public abstract class LoopingThread extends SpyThread {

	/** 
	 * Default number of milliseconds to spend on each loop.
	 */
	public static final int DEFAULT_MS_PER_LOOP=5000;

	private boolean keepGoing=true;
	private int msPerLoop=DEFAULT_MS_PER_LOOP;

	/**
	 * Get an instance of LoopingThread.
	 */
	protected LoopingThread() {
		super();
	}

	/** 
	 * Get a LoopingThread with a specified name.
	 */
	protected LoopingThread(String name) {
		super(name);
	}

	/** 
	 * Get a looping thread belonging to a specific group and having the
	 * specified name.
	 */
	protected LoopingThread(ThreadGroup tg, String name) {
		super(tg, name);
	}

	/** 
	 * Set the number of milliseconds to sleep during each loop.
	 *
	 * Default is 5000 (five seconds).
	 * 
	 * @param msPerLoop number of milliseconds to delay between loops
	 */
	public void setMsPerLoop(int msPerLoop) {
		this.msPerLoop = msPerLoop;
	}

	/** 
	 * Get the number of milliseconds to sleep during each loop.
	 * 
	 * @return the number of milliseconds to wait during a loop.
	 */
	public int getMsPerLoop() {
		return(msPerLoop);
	}

	/** 
	 * Request the looping should end.
	 */
	public void requestStop() {
		keepGoing=false;
		synchronized(this) {
			notifyAll();
		}
	}

	/** 
	 * This is the stuff that should happen during each execution of the
	 * run loop.
	 */
	protected abstract void runLoop();

	/** 
	 * Method to pause between loops.
	 *
	 * This is implemented via wait() so a notify() will cause this object
	 * to awaken from its sleep.
	 */
	protected void performDelay() {
		try {
			synchronized(this) {
				wait(getMsPerLoop());
			}
		} catch(InterruptedException e) {
			getLogger().warn("Somebody interrupted my sleep", e);
		}
	}

	/** 
	 * The run loop itself.
	 */
	public void run() {
		while(keepGoing) {
			// Normally, we'd want to delay before the run loop, but here
			// it doesn't so much matter because there's no exception
			// handling to cause us to hop over the delay.  If the runLoop
			// throws a RuntimeException, we leave the loop.
			runLoop();
			performDelay();
		}
		getLogger().info("Thread finishing.");
	}

}
