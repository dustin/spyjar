// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Cron.java,v 1.5 2003/04/18 07:50:14 dustin Exp $

package net.spy.cron;

import java.io.File;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import net.spy.SpyThread;
import net.spy.util.ThreadPool;

/**
 * Watches a JobQueue and invokes the Jobs when they're ready.
 */
public class Cron extends SpyThread {

	private JobQueue jq=null;
	private boolean stillRunning=true;
	private ThreadPool threads=null;

	// How long we can go idle.
	private long maxIdleTime=900000;
	// Time we last saw a valid job
	private long validJobFound=0;

	/** 
	 * Get a new Cron instance operating on the given queue.
	 * 
	 * @param jq the job queue to watch
	 */
	public Cron(JobQueue jq) {
		this("Cron", jq);
	}

	/** 
	 * Get a new Cron instance with a name, JobQueue and the default thread
	 * pool.
	 * 
	 * @param name name of the cron instance
	 * @param jq the queue to watch
	 */
	public Cron(String name, JobQueue jq) {
		this(name, jq, null);
	}

	/**
	 * Get a new Cron object operating on the given queue.
	 *
	 * @param name thread name
	 * @param jq job queue to watch
	 * @param tp the thread pool
	 */
	public Cron(String name, JobQueue jq, ThreadPool tp) {
		super();
		this.jq=jq;
		setDaemon(true);
		setName(name);

		if(tp==null) {
			tp=new ThreadPool(name + "Pool");
			tp.setStartThreads(5);
			tp.setMinIdleThreads(0);
			tp.setMinTotalThreads(1);
			tp.setMaxTotalThreads(100);
		}
		threads=tp;

		start();
		validJobFound=System.currentTimeMillis();
	}

	/**
	 * Get the current job queue.
	 */
	public JobQueue getJobQueue() {
		return(jq);
	}

	/** 
	 * Shut down the queue.
	 */
	public void shutdown() {
		if(!isRunning()) {
			throw new IllegalStateException("Already shut down");
		}
		stillRunning=false;
		threads.shutdown();
		// XXX:  Write up why I did this.
		synchronized(jq) {
			jq.notify();
		}
	}

	/** 
	 * True if this Cron instance is still running.
	 */
	public boolean isRunning() {
		return(this.stillRunning);
	}

	/** 
	 * Do the run thing.
	 */
	public void run() {
		while(stillRunning) {
			// Check all the running jobs.
			for(Iterator i=jq.getReadyJobs(); i.hasNext(); ) {
				Job j=(Job)i.next();
				getLogger().info("Starting job " + j);
				threads.addTask(j);
			}

			// Just to slow things down a bit.
			yield();

			// Find the soonest job less than a day out.
			long now=System.currentTimeMillis();
			Date next=jq.getNextStartDate();;
			long soonestJob=0;

			// If we didn't get a next job start date, the queue is likely
			// empty.  If we shut down on an empty queue, shut down.
			if(next==null) {
				// If it's been too long, shut down
				if( (now-validJobFound) > maxIdleTime) {
					getLogger().debug("Been a long time "
						+ "since I had a job.  Shutting down.");
					getLogger().debug("now: "+now);
					getLogger().debug("validJobFound: "+validJobFound);
					getLogger().debug("maxIdleTime: "+maxIdleTime);
					shutdown();
				}
				soonestJob=60000;
			} else {
				soonestJob=next.getTime()-now;
				validJobFound=now;
			}

			try {
				if(next!=null) {
					getLogger().debug("Sleeping "
						+ soonestJob + "ms (next job at " + next + ").");
				} else {
					getLogger().debug("Sleeping "
						+ soonestJob + "ms (no good date found).");
				}
				// If we're still running at this point, wait for a job
				if(stillRunning) {
					// Take a second off of the sleep (will use it later)
					soonestJob-=1000;
					// Make sure it's greater than 1, or it'll sleep forever
					if(soonestJob < 1) {
						soonestJob = 1;
					}
					getLogger().debug("Sleeping for " + soonestJob);
					// Sleep on the job
					synchronized(jq) {
						jq.wait(soonestJob);
					}
					// Wait the remaining second
					sleep(1000);
					getLogger().debug("Finished sleep.");
				} else {
					getLogger().debug("Not sleeping, game over.");
				}
			} catch(Exception e) {
				// Don't care, flip faster
				e.printStackTrace();
			}
		} // still running
		getLogger().debug("shut down");
	}

	/** 
	 * Set the maximum amount of time the cron thread will continue running
	 * with no jobs.
	 * 
	 * @param maxIdleTime maximum amount of time in milliseconds
	 */
	public void setMaxIdleTime(long maxIdleTime) {
		this.maxIdleTime=maxIdleTime;
	}

	/** 
	 * Run a Cron instance against a FileJobQueue.
	 */
	public static void main(String args[]) throws Exception {
		FileJobQueue jq=new FileJobQueue(new File(args[0]));
		Cron c=new Cron(jq);

		// Get the time incrementor.
		TimeIncrement ti=new TimeIncrement();
		ti.setField(Calendar.MINUTE);
		ti.setIncrement(2);

		while(c.isAlive()) {
			sleep(10000);
		}
	}
}
