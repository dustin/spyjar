// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ProgressStats.java,v 1.3 2003/06/11 07:31:18 dustin Exp $

package net.spy.util;

import java.util.Date;
import java.text.MessageFormat;

/**
 * A simple class for keeping up with the progress of an operation.
 */
public class ProgressStats extends Object {

	private static final String DEFAULT_FORMAT=
		"{5} remaining.  Avg={0,number,#.##}s, Estimate={1,number,#.##}s "
			+ "({2,date,EEE MMMdd HH:mm:ss})";

	private int done=0;
	private int todo=0;
	private long startTime=0;
	private long totalTime=0;
	private long lastTime=0;
	private long lastProcessTime=0;

	/**
	 * Get an instance of ProgressStats to process the given number of
	 * things.
	 */
	public ProgressStats(int size) {
		super();

		this.startTime=System.currentTimeMillis();
		this.todo=size;
	}

	/** 
	 * Mark us having started processing something.
	 */
	public void start() {
		lastTime=System.currentTimeMillis();
	}

	/** 
	 * Mark this iteration as complete.
	 */
	public void stop() {
		long thistime=System.currentTimeMillis();
		lastProcessTime=thistime-lastTime;
		done++;
		totalTime+=lastProcessTime;
	}

	/** 
	 * Get the overall average.
	 */
	public double getOverallAverage() {
		double avgProcessTime=((double)totalTime/(double)done)/1000.0;
		return(avgProcessTime);
	}

	/** 
	 * Get the estimated number of seconds remaining with the given average
	 * time per unit.
	 */
	public double getEstimatedTimeRemaining(double avgProcessTime) {
		double estimate=avgProcessTime*(double)(todo-done);
		return(estimate);
	}

	/** 
	 * Get the estimated number of seconds remaining with the current
	 * average.
	 */
	public double getEstimatedTimeRemaining() {
		return(getEstimatedTimeRemaining(getOverallAverage()));
	}

	/** 
	 * Get the number of milliseconds spent processing the last item.
	 */
	public long getLastProcessTime() {
		return(lastProcessTime);
	}

	/** 
	 * Get a string representation fo the processing statistics.
	 *
	 * <p>
	 *
	 * The following arguments are available to this format string.
	 *
	 * <ul>
	 *  <li>0: Average processing time in seconds (double)</li>
	 *  <li>1: Estimated seconds to completion (double)</li>
	 *  <li>2: Estimated time of completion (Date)</li>
	 *  <li>3: Number completed (int)</li>
	 *  <li>4: Number total (int)</li>
	 *  <li>5: Number remaining (int)</li>
	 * </ul>
	 *
	 */
	public String getStats(String format) {
		double average=getOverallAverage();
		// Estimated number of seconds left
		double estimate=getEstimatedTimeRemaining(average);
		// Estimated time of completion
		Date ofCompletion=new Date(System.currentTimeMillis()
			+ (1000*(long)estimate));

		// Assemble arguments
		Object args[]={
			new Double(average),
			new Double(estimate),
			ofCompletion,
			new Integer(done),
			new Integer(todo),
			new Integer(todo-done)
			};
		String rv=MessageFormat.format(format, args);
		return(rv);
	}

	/** 
	 * Get a string representation of the processing statistics using the
	 * default format.
	 */
	public String getStats() {
		return(getStats(DEFAULT_FORMAT));
	}

	/** 
	 * String me.
	 * 
	 * @return getStats()
	 */
	public String toString() {
		return(getStats());
	}

}
