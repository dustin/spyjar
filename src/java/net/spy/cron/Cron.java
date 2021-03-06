// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.cron;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import net.spy.SpyThread;
import net.spy.concurrent.ThreadPool;

/**
 * Watches a JobQueue and invokes the Jobs when they're ready.
 */
public final class Cron extends SpyThread {

	private final JobQueue<?> jobQueue;
	private volatile boolean stillRunning=true;
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
	public Cron(JobQueue<?> jq) {
		this("Cron", jq);
	}

	/**
	 * Get a new Cron instance with a name, JobQueue and the default thread
	 * pool.
	 *
	 * @param name name of the cron instance
	 * @param jq the queue to watch
	 */
	public Cron(String name, JobQueue<?> jq) {
		this(name, jq, null);
	}

	/**
	 * Get a new Cron object operating on the given queue.
	 *
	 * @param name thread name
	 * @param jq job queue to watch
	 * @param tp the thread pool
	 */
	public Cron(String name, JobQueue<?> jq, ThreadPool tp) {
		super(new ThreadGroup(name), name);
		this.jobQueue=jq;
		setDaemon(true);
		// Set the thread group to a daemon
		getThreadGroup().setDaemon(true);

		threads=tp;
		if(threads==null) {
			threads=new ThreadPool(name + "Pool", 1, 10, Thread.NORM_PRIORITY,
					new LinkedBlockingQueue<Runnable>(128));
		}

		validJobFound=System.currentTimeMillis();
		start();
	}

	/**
	 * String me.
	 */
	@Override
	public String toString() {
		String extra=null;
		if(jobQueue==null) {
			extra = " - null jobqueue";
		} else {
			extra = " - watching " + jobQueue.size()
				+ " jobs, next up at " + jobQueue.getNextStartDate();
		}
		return(super.toString() + extra);
	}

	/**
	 * Get the current job queue.
	 */
	public JobQueue<?> getJobQueue() {
		return(jobQueue);
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
		synchronized(jobQueue) {
			jobQueue.notifyAll();
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
	@Override
	public void run() {
		getLogger().info("Starting cron services");
		while(stillRunning) {
			// Check all the running jobs.
			for(Job j : jobQueue.getReadyJobs()) {
				getLogger().info("Starting job " + j);
				threads.addTask(j);
			}

			// Find the soonest job less than a day out.
			long now=System.currentTimeMillis();
			Date next=jobQueue.getNextStartDate();
			long soonestJob=0;

			// If we didn't get a next job start date, the queue is likely
			// empty.  If we shut down on an empty queue, shut down.
			if(next==null) {
				getLogger().debug("No job in queue");
				// If it's been too long, shut down
				if( (now-validJobFound) > maxIdleTime) {
					getLogger().info("Been a long time "
						+ "since I had a job.  Shutting down.");
					getLogger().debug("now: %s", now);
					getLogger().debug("validJobFound:  %s", validJobFound);
					getLogger().debug("maxIdleTime:  %s", maxIdleTime);
					shutdown();
				}
				soonestJob=60000;
			} else {
				soonestJob=next.getTime()-now;
				validJobFound=now;
			}

			try {
				if(next!=null) {
					getLogger().debug("Sleeping %sms (next job at %s).",
							soonestJob, next);
				} else {
					getLogger().debug("Sleeping %sms (no good date found).",
							soonestJob);
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
					synchronized(jobQueue) {
						jobQueue.wait(soonestJob);
					}
					// Wait the remaining second
					sleep(1000);
					getLogger().debug("Finished sleep.");
				} else {
					getLogger().debug("Not sleeping, game over.");
				}
			} catch(Exception e) {
				getLogger().warn("Exception in cron loop", e);
			}
		} // still running
		getLogger().info("shut down at");
	}

	/**
	 * Set the maximum amount of time the cron thread will continue running
	 * with no jobs.
	 *
	 * @param to maximum amount of time in milliseconds
	 */
	public void setMaxIdleTime(long to) {
		this.maxIdleTime=to;
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
