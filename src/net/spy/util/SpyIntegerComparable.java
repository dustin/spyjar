// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: SpyIntegerComparable.java,v 1.1 2002/08/28 00:34:57 dustin Exp $

package net.spy.util;


/**
 * Compare Integers for SpySort.
 */
public class SpyIntegerComparable extends Object implements SpyComparable {
	public int compare(Object obj1, Object obj2) {
		Integer o1=null, o2=null;
		int ret=0;

		o1=(Integer)obj1;
		o2=(Integer)obj2;

		int d1=o1.intValue();
		int d2=o2.intValue();

		if(d1 == d2) {
			ret=0;
		} else {
			if(d1 < d2) {
				ret=-1;
			} else {
				ret=1;
			}
		}

		return(ret);
	}
}
