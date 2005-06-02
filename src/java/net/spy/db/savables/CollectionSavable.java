// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 0E0FDEBC-62E4-11D9-AFF7-000A957659CC

package net.spy.db.savables;

import java.util.Collection;

import java.sql.Connection;
import java.sql.SQLException;

import net.spy.db.SaveContext;
import net.spy.db.SaveException;
import net.spy.db.AbstractSavable;

/**
 * Wrapper to save an existing collection.
 */
public class CollectionSavable extends AbstractSavable {

	private Collection collection=null;

	/**
	 * Get an instance of CollectionSavable.
	 */
	public CollectionSavable(Collection c) {
		super();
		this.collection=c;
	}

	/** 
	 * Get the collection.
	 */
	public Collection getPostSavables(SaveContext context) {
		return(collection);
	}

	/** 
	 * NOOP.
	 */
	public void save(Connection conn, SaveContext context)
		throws SaveException, SQLException {
	}

}
