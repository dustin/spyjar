// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 89A2F86F-1110-11D9-B0B6-000A957659CC

package net.spy.util;

import java.util.Date;

/**
 * Compare Dates for SpySort.
 */
public class SpyDateComparable extends Object implements SpyComparable {
	public int compare(Object obj1, Object obj2) {
		Date o1=null, o2=null;
		int ret=0;

		o1=(Date)obj1;
		o2=(Date)obj2;

		if(o1.before(o2)) {
			ret=-1;
		} else if(o1.after(o2)) {
			ret=1;
		} else {
			ret=0;
		}

		return(ret);
	}
}
