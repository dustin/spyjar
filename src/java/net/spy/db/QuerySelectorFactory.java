// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6C7C6CCE-1110-11D9-8498-000A957659CC

package net.spy.db;

import net.spy.SpyObject;
import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * Factory for finding a QuerySelector instance.
 *
 * If the system propery <code>net.spy.db.QuerySelector</code> is set, it
 * will point to the QuerySelector implementation to use, otherwise
 * {@link net.spy.db.DatabaseMetaDataQuerySelector} is used.
 */
public class QuerySelectorFactory extends SpyObject {

	private static QuerySelector qs=null;

	private static final String PROPERTY_NAME=
		"net.spy.db.QuerySelector";
	private static final String DEFAULT_SELECTOR=
		"net.spy.db.DatabaseMetaDataQuerySelector";

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
				Logger l=LoggerFactory.getLogger(QuerySelectorFactory.class);
				l.warn("Couldn't make a " + selectorClassName
					+ ", using " + DEFAULT_SELECTOR, e);
				qs=new DatabaseMetaDataQuerySelector();
			}
		}
	}

}
