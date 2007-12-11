// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

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

	private final Collection<? extends Savable> collection;

	/**
	 * Get an instance of CollectionSavable.
	 */
	public CollectionSavable(Collection<? extends Savable> c) {
		super();
		collection=c;
	}

	/**
	 * Get the collection.
	 */
	@Override
	public Collection<? extends Savable> getPostSavables(SaveContext context) {
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
