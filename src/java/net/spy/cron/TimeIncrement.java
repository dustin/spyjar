// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 641D8450-1110-11D9-9FD7-000A957659CC

package net.spy.cron;

import java.util.Calendar;
import java.util.Date;

/**
 * This object defines how a recurring job should increment itself.
 */
public class TimeIncrement extends Object {

	private int fieldIncrement=0;
	private int incAmount=0;
	private final Calendar calendar;

	/**
	 * Get a new TimeIncrement object.
	 */
	public TimeIncrement() {
		this(Calendar.getInstance());
	}

	/**
	 * Get a new TimeIncrement object using a specific Calendar.
	 */
	public TimeIncrement(Calendar c) {
		super();
		calendar=c;
	}

	/** 
	 * String me.
	 */
	@Override
	public String toString() {
		return("{" + getClass().getName() + " field=" + fieldIncrement
			+ ", amount=" + incAmount + "}");
	}

	/**
	 * Set Calendar field to increment.
	 */
	public void setField(int to) {
		fieldIncrement=to;
	}

	/**
	 * Set the increment amount.
	 */
	public void setIncrement(int to) {
		incAmount=to;
	}

	/**
	 * Get the next value of this Date.
	 */
	public Date nextDate(Date d) {
		Date rv=d;

		// Keep incrementing until we've got the thing into the future.
		long now=System.currentTimeMillis();
		while(rv.getTime()<=now) {
			calendar.setTime(rv);
			calendar.add(fieldIncrement, incAmount);
			rv=calendar.getTime();
		}

		return(rv);
	}

}
