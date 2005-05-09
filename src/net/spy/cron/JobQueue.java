// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6341734B-1110-11D9-8968-000A957659CC

package net.spy.cron;

import java.util.Date;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * This is where all the jobs go.
 */
public class JobQueue<T extends Job> extends ArrayList<T> {

	private Logger logger=null;

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
	public synchronized void addJob(T j) {
		if(getLogger().isDebugEnabled()) {
			getLogger().debug("Adding job:  " + j);
		}
		add(j);
		notify();
	}

	/**
	 * Get an Iterator of Jobs that are ready to run.
	 */
	public synchronized Collection<Job> getReadyJobs() {
		ArrayList v=new ArrayList();

		// Flip through all of the jobs and see what we've got to do.
		for(Iterator<T> i=iterator(); i.hasNext();) {
			Job j=i.next();

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

		return(v);
	}

	/**
	 * Get the time the next job will start.
	 */
	public synchronized Date getNextStartDate() {
		Date next=null;
		long soonestJob=Long.MAX_VALUE;
		long now=System.currentTimeMillis();
		for(Job j : this) {
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
