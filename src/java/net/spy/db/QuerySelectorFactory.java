// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

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
				@SuppressWarnings("unchecked")
				Class<? extends QuerySelector> c
					=(Class<? extends QuerySelector>) Class.forName(
							selectorClassName);
				qs=c.newInstance();
			} catch(Exception e) {
				Logger l=LoggerFactory.getLogger(QuerySelectorFactory.class);
				l.warn("Couldn't make a %s, using %s", selectorClassName,
						DEFAULT_SELECTOR, e);
				qs=new DatabaseMetaDataQuerySelector();
			}
		}
	}

}
