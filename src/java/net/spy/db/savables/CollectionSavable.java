// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 0E0FDEBC-62E4-11D9-AFF7-000A957659CC

package net.spy.db.savables;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import net.spy.db.AbstractSavable;
import net.spy.db.Savable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;

/**
 * Wrapper to save an existing collection.
 */
public class CollectionSavable extends AbstractSavable {

	private Collection<Savable> collection=null;

	/**
	 * Get an instance of CollectionSavable.
	 */
	public CollectionSavable(Collection<Savable> c) {
		super();
		this.collection=c;
	}

	/** 
	 * Get the collection.
	 */
	public Collection<Savable> getPostSavables(SaveContext context) {
		return(collection);
	}

	/** 
	 * NOOP.
	 */
	public void save(Connection conn, SaveContext context)
		throws SaveException, SQLException {
		// No implementation necessary since this object itself won't be saved
	}

}
