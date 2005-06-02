// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 63D8662B-1110-11D9-B7CC-000A957659CC

package net.spy.cron;

import java.util.Calendar;

/**
 * A simple time increment implemenation.
 */
public class SimpleTimeIncrement extends TimeIncrement {

	/**
	 * Get an instance of SimpleTimeIncrement.
	 *
	 * @param ms The number of milliseconds to sleep.
	 */
	public SimpleTimeIncrement(int ms) {
		super();
		setField(Calendar.MILLISECOND);
		setIncrement(ms);
	}

}
