// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Saver.java,v 1.4 2003/01/07 07:04:21 dustin Exp $

package net.spy.db;

import java.util.Iterator;
import java.util.Map;
import java.util.IdentityHashMap;
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
	private Map listedObjects=null;

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
		this.listedObjects=new IdentityHashMap();
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
		} catch(SaveException se) {
			throw se;
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
		for(Iterator i=listedObjects.values().iterator(); i.hasNext(); ) {
			Object otmp=i.next();
			if(otmp instanceof TransactionListener) {
				TransactionListener tl=(TransactionListener)otmp;
				tl.transactionCommited();
			}
		} // end looking at all the listeners.
	}

	// Deal with individual saves.
	private void rsave(Savable o) throws SaveException, SQLException {
		rdepth++;

		// watch recursion depth
		if(rdepth>MAX_RECURSION_DEPTH) {
			throw new SaveException("Recursing too deep!  Max depth is  "
				+ MAX_RECURSION_DEPTH);
		}

		// Save this object if it needs saving.
		if(o.isNew() || o.isModified()) {
			o.save(conn, context);
		}

		// Only go through the savables if we haven't gone through the
		// savables for this exact object
		if(!listedObjects.containsKey(o)) {
			// Add this to the set to keep us from doing it again
			listedObjects.put(o, null);

			// Get the savables
			Collection c=o.getSavables(context);
			if(c!=null) {

				// Deal with the savables
				for(Iterator i=c.iterator(); i.hasNext(); ) {
					Savable s=(Savable)i.next();
					// verify the object isn't null, if it is, report it here
					// so we can figure out what gave it to us
					if(s==null) {
						throw new NullPointerException("Got a null object from "
							+ o);
					}

					rsave(s);
				} // Flipping through the savables
			} // getSavables returned a collection
		} // Haven't seen this object

		rdepth--;
	}

}
