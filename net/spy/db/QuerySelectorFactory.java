// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: QuerySelectorFactory.java,v 1.1 2002/10/30 08:16:34 dustin Exp $

package net.spy.db;

/**
 * Factory for finding a QuerySelector instance.
 *
 * If the system propery <code>net.spy.db.QuerySelector</code> is set, it
 * will point to the QuerySelector implementation to use, otherwise
 * {@link net.spy.db.DefaultQuerySelector} is used.
 */
public class QuerySelectorFactory extends Object {

	private static QuerySelector qs=null;

	private static final String PROPERTY_NAME=
		"net.spy.db.QuerySelector";
	private static final String DEFAULT_SELECTOR=
		"net.spy.db.DefaultQuerySelector";

	/** 
	 * Get the QuerySelector instance.
	 */
	public static QuerySelector getQuerySelector() {
		initQuerySelector();
		return (qs);
	}

	private static synchronized void initQuerySelector() {
		if(qs == null) {
			String selectorClassName=System.getProperty(
				PROPERTY_NAME,
				DEFAULT_SELECTOR);
			try {
				Class c=Class.forName(selectorClassName);
				qs=(QuerySelector)c.newInstance();
			} catch(Exception e) {
				e.printStackTrace();
				System.err.println("Using " + DEFAULT_SELECTOR);
				qs=new DefaultQuerySelector();
			}
		}
	}

}
