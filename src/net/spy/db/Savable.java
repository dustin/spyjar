// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6D07762E-1110-11D9-996F-000A957659CC

package net.spy.db;

import java.util.Collection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for transactionally savable objects.
 *
 * @deprecated use SavableNode instead
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
	 * Get a list of all of the Savables this Savable is holding that will
	 * need to be saved after this object.
	 *
	 * @param context SaveContext being used in this Saver session
	 * @return a collection of dependent objects to save, or null if there
	 * 			are no dependent objects
	 */
	Collection getSavables(SaveContext context);

}
