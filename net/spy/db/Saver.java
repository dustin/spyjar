// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Saver.java,v 1.6 2003/01/15 08:08:06 dustin Exp $

package net.spy.db;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.sql.Connection;
import java.sql.SQLException;

import net.spy.SpyDB;
import net.spy.SpyConfig;
import net.spy.SpyObject;

/**
 * Transactional object saver.
 */
public class Saver extends SpyObject {

	private static final int MAX_RECURSION_DEPTH=100;

	private SaveContext context=null;
	private SpyConfig config=null;
	private int rdepth=0;

	// Make sure we don't deal with the same object more than once
	private Set listedObjects=null;

	private SpyDB db=null;
	private Connection conn=null;

	/**
	 * Get an instance of Saver with the given database config.
	 */
	public Saver(SpyConfig config) {
		this(config, new SaveContext());
	}

	/**
	 * Get an instance of saver with the given database config and context.
	 */
	public Saver(SpyConfig config, SaveContext context) {
		super();
		this.context=context;
		this.config=config;
		this.listedObjects=new HashSet();
	}

	/**
	 * Save this Savabale and everything it contains.
	 */
	public void save(Savable o) throws SaveException {
		boolean complete=false;

		try {
			db=new SpyDB(config);
			conn=db.getConn();
			conn.setAutoCommit(false);

			// Begin recursion
			rsave(o);

			complete=true;
		} catch(SQLException se) {
			throw new SaveException("Error saving object", se);
		} finally {
			// figure out whether we need to commit or roll back
			if(conn!=null) {
				if(complete==false) {
					try {
						conn.rollback();
					} catch(SQLException se) {
						getLogger().warn("Problem rolling back.", se);
					}
				} else {
					try {
						conn.commit();
					} catch(SQLException se) {
						throw new SaveException("Error committing", se);
					}
				}

				// Reset autocommit state
				try {
					conn.setAutoCommit(true);
				} catch(SQLException sqe) {
					getLogger().warn("Problem resetting autocommit.", sqe);
				}
			} // Dealt with opened connection

			// Return a connection to the pool
			if(db!=null) {
				db.close();
			}
		}

		// Inform all of the TransactionListener objects that the
		// transaction is complete.
		for(Iterator i=listedObjects.iterator(); i.hasNext(); ) {
			IdentityEqualifier ie=(IdentityEqualifier)i.next();
			if(ie.get() instanceof TransactionListener) {
				TransactionListener tl=(TransactionListener)ie.get();
				tl.transactionCommited();
			}
		} // end looking at all the listeners.
	}

	// Deal with individual saves.
	private void rsave(Savable o) throws SaveException, SQLException {
		rdepth++;

		// watch recursion depth
		if(rdepth>MAX_RECURSION_DEPTH) {
			throw new SaveException("Recursing too deep!  Max depth is "
				+ MAX_RECURSION_DEPTH);
		}

		// Only go through the savables if we haven't gone through the
		// savables for this exact object
		IdentityEqualifier ie=new IdentityEqualifier(o);
		if(!listedObjects.contains(ie)) {
			// Add this to the set to keep us from doing it again
			listedObjects.add(ie);

			// If this is a SavableNode, it's got a pre, grab those
			if(o instanceof SavableNode) {
				SavableNode sn=(SavableNode)o;
				saveLoop(o, sn.getPreSavables(context));
			} // Was a savable node

			// Save this object if it needs saving.
			if(o.isNew() || o.isModified()) {
				o.save(conn, context);
			}

			// Get the post savables
			saveLoop(o, o.getSavables(context));

		} // Haven't seen this object

		rdepth--;
	}

	// Loop through a Collection of savables, passing each to rsave().  If
	// only java supported functional programming...
	private void saveLoop(Savable o, Collection c)
		throws SaveException, SQLException {
		if(c!=null) {
			for(Iterator i=c.iterator(); i.hasNext(); ) {
				Savable s=(Savable)i.next();
				if(s==null) {
					throw new NullPointerException("Got a null object from "
						+ o);
				}

				rsave(s);
			} // iterator loop
		} // got a collection
	} // saveLoop()

	// private class to deal with identity comparisons
	private class IdentityEqualifier {

		private Object o=null;

		// Get an IdentityEqualifier
		public IdentityEqualifier(Object o) {
			super();
			this.o=o;
		}

		// Get the object
		public Object get() {
			return(o);
		}

		// Identity comparison
		public boolean equals(Object other) {
			return(o == other);
		}

	}

}
