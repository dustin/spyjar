// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: MainJob.java,v 1.2 2002/11/20 04:32:07 dustin Exp $

package net.spy.cron;

import java.util.Date;

import net.spy.SpyUtil;

/**
 * A job that invokes a class's main() method at run time.
 */
public class MainJob extends Job {

	// The classname and the args to run.
	private String classname=null;
	private String args[]=null;

	/**
	 * Get a new ``at style'' MainJob.
	 */
	public MainJob(String classname, String args[], Date startDate) {
		super("main:" + classname, startDate);
		this.classname=classname;
		this.args=args;
	}

	/**
	 * Get a new ``cron style'' MainJob.
	 */
	public MainJob(String classname, String args[],
		Date startDate, TimeIncrement ti) {
		super("main:" + classname, startDate, ti);
		this.classname=classname;
		this.args=args;
	}

	/**
	 * What to do when it's time to run.
	 */
	public void runJob() {
		try {
			SpyUtil.runClass(classname, args);
		} catch(Exception e) {
			getLogger().error("Problem invoking main class " + classname, e);
		}
	}

}
