// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: SavableHashSet.java,v 1.1 2003/01/15 20:57:51 dustin Exp $

package net.spy.db.savables;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.HashSet;
import java.util.Collection;

import net.spy.db.SaveException;
import net.spy.db.SavableNode;
import net.spy.db.SaveContext;

/**
 * A subclass of HashSet that implements SavableNode.
 *
 * The save() method does nothing (and should not be called), but all of
 * the objects in the Set will be returned from getPostSavables().
 *
 * @author <a href="mailto:dsallings@2wire.com">Dustin Sallings</a>
 */
public class SavableHashSet extends HashSet implements SavableNode {

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
	public SavableHashSet(Collection col) {
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
	public Collection getPreSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * @return this
	 */
	public Collection getPostSavables(SaveContext context) {
		return(this);
	}

	/** 
	 * @return this
	 */
	public Collection getSavables(SaveContext context) {
		return(this);
	}
}
