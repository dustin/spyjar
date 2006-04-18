// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6D07762E-1110-11D9-996F-000A957659CC

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Interface for transactionally savable objects.
 */
public interface Savable {

	/**
	 * Is this a new object?
	 */
	boolean isNew();

	/**
	 * Has this object been modified?
	 */
	boolean isModified();

	/**
	 * Save this object's state over the given connection.
	 *
	 * @param conn the connection to use for saving
	 * @param context SaveContext being used in this Saver session
	 */
	void save(Connection conn, SaveContext context)
		throws SaveException, SQLException;

	/** 
	 * Get a Collection of all of the SavableNodes this SavableNode is
	 * holding that will need to be saved before this Savable.
	 * 
	 * @param context SaveContext being used in this Saver session
	 * @return a collection of objects this SavableNode depends on
	 */
	Collection<Savable> getPreSavables(SaveContext context);

	/** 
	 * Get a Collection of all of the SavableNodes this SavableNode is
	 * holding that will need to be saved after this Savable.
	 * 
	 * @param context SaveContext being used in this Saver session
	 * @return a collection of objects this SavableNode depends on
	 */
	Collection<Savable> getPostSavables(SaveContext context);

}
