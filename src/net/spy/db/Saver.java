// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Saver.java,v 1.14 2004/02/06 01:46:58 dustin Exp $

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

import net.spy.util.IdentityEqualifier;

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
		this(config, null);
	}

	/**
	 * Get an instance of saver with the given database config and context.
	 */
	public Saver(SpyConfig config, SaveContext context) {
		super();
		this.context=context;
		if(this.context == null) {
			this.context = new SaveContext();
		}
		this.config=config;
		this.listedObjects=new HashSet();
	}

	/** 
	 * Save this Savabale and everything it contains at the default isolation
	 * level.
	 */
	public void save(Savable o) throws SaveException {
		save(o, null);
	}

	/** 
	 * Save this Savabale and everything it contains at the default isolation
	 * level.
	 *
	 * @param o the savable
	 * @param level the isolation level (as defined in java.sql.Connection)
	 */
	public void save(Savable o, int level) throws SaveException {
		save(o, new Integer(level));
	}

	/**
	 * Save this Savabale and everything it contains.
	 */
	private void save(Savable o, Integer isoLevel) throws SaveException {
		boolean complete=false;

		int oldIsolationLevel=0;

		if(getLogger().isDebugEnabled()) {
			getLogger().debug("Beginning save transaction " + getSessId()
				+ " with isolation level " + isoLevel);
		}

		try {
			db=new SpyDB(config);
			conn=db.getConn();
			if(isoLevel != null) {
				oldIsolationLevel=conn.getTransactionIsolation();
				conn.setTransactionIsolation(isoLevel.intValue());
			}
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
				// Reset the isolation level
				if(isoLevel != null) {
					try {
						conn.setTransactionIsolation(oldIsolationLevel);
					} catch(SQLException sqe) {
						getLogger().warn("Problem resetting isolation level.",
							sqe);
					}
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
		if(getLogger().isDebugEnabled()) {
			getLogger().debug("Save transaction " + getSessId() + " complete");
		}
	}

	private String getSessId() {
		return(Integer.toHexString(context.getId()));
	}

	// Deal with individual saves.
	private void rsave(Savable o) throws SaveException, SQLException {
		rdepth++;

		checkRecursionDepth();

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
				// Log the pre-save
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Saving " + dbgString(o)
						+ " in " + getSessId());
				}

				// Perform the actual save
				o.save(conn, context);

				// Log the post save
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Completed saving " + dbgString(o)
						+ " in " + getSessId());
				}
			} else {
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Not saving " + dbgString(o)
						+ " in " + getSessId() + " (not modified)");
				}
			}

			// Get the post savables
			saveLoop(o, o.getSavables(context));
			// This is a kind of ugly hack, but he identity equalifier
			// should prevent the same object from being saved more than
			// once, even though the tree is walked twice.  Perhaps
			// getPostSavables was a mistake to wedge in for backwards
			// compatibility.  Maybe I should go for a one or the other
			// type implementation.  In the meantime, this scared the shit
			// out of me.
			if(o instanceof SavableNode) {
				SavableNode sn=(SavableNode)o;
				saveLoop(o, sn.getPostSavables(context));
			}

		} // Haven't seen this object

		rdepth--;
	}

	private void checkRecursionDepth() throws SaveException {
		if(rdepth>MAX_RECURSION_DEPTH) {
			throw new SaveException("Recursing too deep!  Max depth is "
				+ MAX_RECURSION_DEPTH);
		}
	}

	//  make a pleasant way to print out the debug strings.
	private String dbgString(Object o) {
		StringBuffer sb=new StringBuffer(80);
		if(o == null) {
			sb.append("<null>");
		} else {
			sb.append("{");
			sb.append(o.getClass().getName());
			sb.append("@");
			sb.append(Integer.toHexString(System.identityHashCode(o)));
			String s=o.toString();
			sb.append(" - ");
			if(s.length() < 64) {
				sb.append(s);
			} else {
				sb.append(s.substring(0, 63));
			}
			sb.append("}");
		}
		return(sb.toString());
	}

	// Loop through a Collection of savables, passing each to rsave().  If
	// only java supported functional programming...
	private void saveLoop(Savable o, Collection c)
		throws SaveException, SQLException {

		rdepth++;

		checkRecursionDepth();

		if(c!=null) {
			for(Iterator i=c.iterator(); i.hasNext(); ) {
				Object tmpo=i.next();
				if(tmpo==null) {
					throw new NullPointerException("Got a null object from "
						+ o + " (" + dbgString(o) + ")");
				}

				// Dispatch based on type
				if(tmpo instanceof Savable) {
					rsave((Savable)tmpo);
				} else if(tmpo instanceof Collection) {
					saveLoop(o, (Collection)tmpo);
				} else {
					throw new SaveException(
						"Invalid object type found in save tree:  "
						+ tmpo.getClass() + " from " + o
						+ " (" + dbgString(o) + ")");
				}
			} // iterator loop
		} // got a collection

		rdepth--;
	} // saveLoop()

}
