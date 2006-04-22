// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 836DC6FD-1110-11D9-8C0C-000A957659CC

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
	 * @param to number of milliseconds to delay between loops
	 */
	public void setMsPerLoop(int to) {
		this.msPerLoop = to;
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
	public synchronized void requestStop() {
		// notify so the wait will finish
		keepGoing=false;
		notifyAll();
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
		startingUp();
		while(keepGoing) {
			// Normally, we'd want to delay before the run loop, but here
			// it doesn't so much matter because there's no exception
			// handling to cause us to hop over the delay.  If the runLoop
			// throws a RuntimeException, we leave the loop.
			runLoop();
			performDelay();
		}
		getLogger().info("Thread finishing.");
		shuttingDown();
	}

	/** 
	 * Hook method invoked when the thread is starting up.
	 */
	protected void startingUp() {
	}

	/** 
	 * Hook method invoked when the thread is shutting down.
	 */
	protected void shuttingDown() {
	}

}
