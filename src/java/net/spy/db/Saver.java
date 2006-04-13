// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6E505332-1110-11D9-9D6C-000A957659CC

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.spy.SpyObject;
import net.spy.util.IdentityEqualifier;
import net.spy.util.SpyConfig;

/**
 * Transactional object saver.
 */
public class Saver extends SpyObject {

	private static final int MAX_RECURSION_DEPTH=100;

	private SaveContext context=null;
	private SpyConfig config=null;
	private int rdepth=0;

	// Make sure we don't deal with the same object more than once
	private Set<IdentityEqualifier> listedObjects=null;

	private ConnectionSource connSrc=null;
	private Connection conn=null;

	/**
	 * Get an instance of Saver with the given database config.
	 */
	public Saver(SpyConfig conf) {
		this(conf, null);
	}

	/**
	 * Get an instance of saver with the given database config and context.
	 */
	public Saver(SpyConfig conf, SaveContext ctx) {
		super();
		this.context=ctx;
		if(this.context == null) {
			this.context = new SaveContext();
		}
		this.config=conf;
		ConnectionSourceFactory csf=ConnectionSourceFactory.getInstance();
		connSrc=csf.getConnectionSource(conf);
		this.listedObjects=new HashSet<IdentityEqualifier>();
	}

	/** 
	 * Save this Savabale and everything it contains at the default isolation
	 * level.
	 */
	public void save(Savable o) throws SaveException {
		save(o, null);
	}

	/**
	 * Save this Savabale and everything it contains.
	 */
	public void save(Savable o, Integer isoLevel) throws SaveException {
		boolean complete=false;
		listedObjects.clear();

		int oldIsolationLevel=0;

		getLogger().debug(
				"Beginning save transaction %s with isolation level %s",
				getSessId(), isoLevel);

		try {
			conn=connSrc.getConnection(config);
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
			if(conn != null) {
				connSrc.returnConnection(conn);
			}
		}

		// Inform all of the TransactionListener objects that the
		// transaction is complete.
		for(IdentityEqualifier ie : listedObjects) {
			if(ie.get() instanceof TransactionListener) {
				TransactionListener tl=(TransactionListener)ie.get();
				tl.transactionCommited();
			}
		} // end looking at all the listeners.
		getLogger().debug("Save transaction %s complete", getSessId());
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

			// Go through the preSavables
			saveLoop(o, o.getPreSavables(context));

			// Save this object if it needs saving.
			if(o.isNew() || o.isModified()) {
				// Log the pre-save
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Saving %s in %s",
							dbgString(o), getSessId());
				}

				// Perform the actual save
				o.save(conn, context);

				// Log the post save
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Completed saving %s in %s",
							dbgString(o), getSessId());
				}
			} else {
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Not saving %s in %s (not modified)",
							dbgString(o), getSessId());
				}
			}

			// Get the post savables
			saveLoop(o, o.getPostSavables(context));

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
		StringBuilder sb=new StringBuilder(80);
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
	private void saveLoop(Savable o, Collection<?> name)
		throws SaveException, SQLException {

		rdepth++;

		checkRecursionDepth();

		if(name!=null) {
			for(Object tmpo : name) {
				if(tmpo==null) {
					throw new NullPointerException("Got a null object from "
						+ o + " (" + dbgString(o) + ")");
				}

				// Dispatch based on type
				if(tmpo instanceof Savable) {
					rsave((Savable)tmpo);
				} else if(tmpo instanceof Collection<?>) {
					saveLoop(o, (Collection<?>)tmpo);
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
