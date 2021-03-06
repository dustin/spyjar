// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>

package net.spy.db.savables;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.spy.db.Savable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;

/**
 * A subclass of HashMap that implements Savable.
 *
 * The save() method does nothing (and should not be called), but all of
 * the values in the Map will be returned from getPostSavables().
 *
 * @author <a href="mailto:dsallings@2wire.com">Dustin Sallings</a>
 */
public class SavableHashMap<K, V extends Savable> extends HashMap<K, V>
	implements Savable {

    /**
     * Get an instance of SavableHashMap.
     */
    public SavableHashMap() {
        super();
    }

	/**
	 * Get an instance of SavableHashMap populated with the given
	 * Map of objects.
	 */
	public SavableHashMap(Map<K, V> map) {
		super(map);
	}

	/**
	 * Get an instance of SavableHashMap with the given initial capacity.
	 */
	public SavableHashMap(int initCap) {
		super(initCap);
	}

	/**
	 * Get an instance of SavableHashMap with the given initial capacity
	 * and load factors.
	 */
	public SavableHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	// Savable implementation

	/**
	 * @return false
	 */
	public boolean isNew() {
		return(false);
	}

	/**
	 * @return false
	 */
	public boolean isModified() {
		return(false);
	}

	/**
	 * Do nothing.
	 */
	public void save(Connection conn, SaveContext context)
		throws SaveException, SQLException {

		// Ignored
	}

	/**
	 * @return null
	 */
	public Collection<? extends Savable> getPreSavables(SaveContext context) {
		return(null);
	}

	/**
	 * @return values()
	 */
	public Collection<? extends Savable> getPostSavables(SaveContext context) {
		return(values());
	}

}
