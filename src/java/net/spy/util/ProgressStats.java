// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 850AD394-1110-11D9-A061-000A957659CC

package net.spy.util;

import java.text.MessageFormat;
import java.util.Date;

/**
 * A simple class for keeping up with the progress of an operation.
 */
public class ProgressStats extends Object {

	private static final String DEFAULT_FORMAT=
		"{5} remaining.  Avg={0,number,#.##}s, Estimate={1,number,#.##}s "
			+ "({2,date,EEE MMMdd HH:mm:ss})";

	private int done=0;
	private int todo=0;
	private long totalTime=0;
	private long lastTime=0;
	private long lastProcessTime=0;

	private double avg=0.0;

	/**
	 * Get an instance of ProgressStats to process the given number of
	 * things.
	 */
	public ProgressStats(int size) {
		super();

		this.todo=size;
	}

	/**
	 * Get the current time for this ProgressStats instance.  This exists as a
	 * test fixture, but if you want to do something more exciting with it,
	 * go right ahead.
	 */
	protected long getTime() {
		return System.currentTimeMillis();
	}

	/**
	 * Mark us having started processing something.
	 */
	public void start() {
		lastTime=getTime();
	}

	/** 
	 * Mark this iteration as complete.
	 */
	public void stop() {
		long thistime=getTime();
		lastProcessTime=thistime-lastTime;
		done++;
		totalTime+=lastProcessTime;

		double df=done;
		double v=lastProcessTime / 1000.0;

		avg = ((avg * df) + v) / (df + 1);
	}

	/** 
	 * Get the running average of the number of seconds spent processing.
	 * @return ((avg * df) + v) / (df + 1)
	 */
	public double getRunningAverage() {
		return(avg);
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
		double estimate=avgProcessTime*(todo-done);
		return(estimate);
	}

	/** 
	 * Get the estimated number of seconds remaining with the current
	 * average.
	 */
	public double getEstimatedTimeRemaining() {
		return(getEstimatedTimeRemaining(getRunningAverage()));
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
		double average=getRunningAverage();
		// Estimated number of seconds left
		double estimate=getEstimatedTimeRemaining(average);
		// Estimated time of completion
		Date ofCompletion=new Date(getTime() + (1000*(long)estimate));

		// Assemble arguments
		Object[] args={
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
