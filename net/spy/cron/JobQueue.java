// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: JobQueue.java,v 1.5 2003/04/21 02:57:46 dustin Exp $

package net.spy.cron;

import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * This is where all the jobs go.
 */
public class JobQueue extends ArrayList {

	private Logger logger=null;

	// Loop at least this frequently (twenty-four hours)
	private static final long MAX_SLEEP_TIME=86400000;

	/**
	 * Get a new job queue.
	 */
	public JobQueue() {
		super();
	}

	/** 
	 * Get the logger for this instance.
	 * 
	 * @return the appropriate logger for this object
	 */
	protected Logger getLogger() {
		if(logger==null) {
			logger=LoggerFactory.getLogger(getClass());
		}
		return(logger);
	}

	/**
	 * Add a job.
	 */
	public synchronized void addJob(Job j) {
		add(j);
		synchronized(this) {
			notify();
		}
	}

	/**
	 * Get an Iterator of Jobs that are ready to run.
	 */
	public synchronized Iterator getReadyJobs() {
		ArrayList v=new ArrayList();

		// Flip through all of the jobs and see what we've got to do.
		for(Iterator i=iterator(); i.hasNext(); ) {
			Job j=(Job)i.next();

			// Add a job if it's ready.
			if(j.isReady()) {
				v.add(j);
				// Reschedule the job
				j.findNextRun();
			} else if(j.isTrash()) {
				getLogger().info("JobQueue: Removing " + j);
				i.remove();
			}
		}

		return(v.iterator());
	}

	/**
	 * Get the time the next job will start.
	 */
	public Date getNextStartDate() {
		Date next=null;
		long soonestJob=MAX_SLEEP_TIME;
		long now=System.currentTimeMillis();
		for(Iterator i=iterator(); i.hasNext(); ) {
			Job j=(Job)i.next();
			Date jdate=j.getStartTime();
			if(jdate!=null) {
				long t=jdate.getTime()-now;
				if(t>0 && t<soonestJob) {
					soonestJob=t;
					next=jdate;
				}
			}
		}

		return(next);
	}

}
