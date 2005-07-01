// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 639281E0-1110-11D9-A286-000A957659CC

package net.spy.cron;

import java.util.Date;

import net.spy.util.SpyUtil;
import net.spy.util.ThreadPoolRunnable;

/**
 * A job that invokes a class's main() method at run time.
 */
public class MainJob extends Job implements ThreadPoolRunnable {

	// The classname and the args to run.
	private String classname=null;
	private String args[]=null;

	/**
	 * Get a new ``at style'' MainJob.
	 */
	public MainJob(String cName, String a[], Date startDate) {
		super("main:" + cName, startDate);
		this.classname=cName;
		this.args=a;
	}

	/**
	 * Get a new ``cron style'' MainJob.
	 */
	public MainJob(String cName, String a[],
		Date startDate, TimeIncrement ti) {
		super("main:" + cName, startDate, ti);
		this.classname=cName;
		this.args=a;
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
