// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: B8B36B20-1110-11D9-B831-000A957659CC

package net.spy.db.savables;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

import net.spy.db.Savable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;

/**
 * A subclass of HashSet that implements Savable.
 *
 * The save() method does nothing (and should not be called), but all of
 * the objects in the Set will be returned from getPostSavables().
 *
 * @author <a href="mailto:dsallings@2wire.com">Dustin Sallings</a>
 */
public class SavableHashSet extends HashSet<Savable> implements Savable {

    /**
     * Get an instance of SavableHashSet.
     */
    public SavableHashSet() {
        super();
    }

	/** 
	 * Get an instance of SavableHashSet populated with the given
	 * Collection of objects.
	 */
	public SavableHashSet(Collection<Savable> col) {
		super(col);
	}

	/** 
	 * Get an instance of SavableHashSet with the given initial capacity.
	 */
	public SavableHashSet(int initCap) {
		super(initCap);
	}

	/** 
	 * Get an instance of SavableHashSet with the given initial capacity
	 * and load factors.
	 */
	public SavableHashSet(int initialCapacity, float loadFactor) {
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
	public Collection<Savable> getPreSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * @return this
	 */
	public Collection<Savable> getPostSavables(SaveContext context) {
		return(this);
	}
}
