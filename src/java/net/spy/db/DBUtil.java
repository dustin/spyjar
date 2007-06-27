// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

public final class DBUtil {

	// Count the number of question marks
	public static int countQs(String query) {
		int qs=0;
		int currentIndex=-1;

		do {
			currentIndex=query.indexOf("?", currentIndex+1);
			if(currentIndex>=0) {
				qs++;
			}
		} while(currentIndex>=0);

		return(qs);
	}
}
