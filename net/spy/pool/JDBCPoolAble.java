//
// $Id: JDBCPoolAble.java,v 1.3 2002/12/15 07:26:49 dustin Exp $

package net.spy.pool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * PoolAble object for containing a JDBC object.
 */
public class JDBCPoolAble extends PoolAble {

	/**
	 * Get a JDBC poolable.
	 */
	public JDBCPoolAble(Object theObject, int poolHash) {
		super(theObject, poolHash);
	}

	/**
	 * Get a JDBC poolable.
	 */
	public JDBCPoolAble(Object theObject, long maxAge, int poolHash) {
		super(theObject, maxAge, poolHash);
	}

	/**
	 * @see PoolAble
	 */
	public void discard() {
		try {
			Connection c=(Connection)intGetObject();
			if(c!=null) {
				c.close();
			}
		} catch(Exception e) {
			getLogger().error("Error on finalize!  ObjectID="
				+ getObjectID() + ":  ", e);
		}
		// Tell the parent to do the same.
		super.discard();
	}

	/**
	 * Find out of the JDBCPoolAble represents a usable object.  This is
	 * done by doing a select of a constant and verifying it gets the same
	 * value in the return.
	 *
	 * @return true if the object will be usable
	 */
	public boolean isAlive() {
		boolean ret=false;
		try {
			Connection c=(Connection)intGetObject();
			Statement st=c.createStatement();
			ResultSet rs=st.executeQuery("select 7");
			rs.next();
			int r=rs.getInt(1);
			// Set the value to true only if r==7
			ret=(r==7);
			rs.close();
			st.close();
		} catch(Exception e) {
			// Turn off availability
			setUnavailable();
		}
		return(ret);
	}

	/**
	 * Overridden to deal with SQL Warnings.
	 *
	 * @see PoolAble
	 */
	public synchronized void checkIn() {
		try {
			Connection c=(Connection)intGetObject();
			if(c!=null) {
				SQLWarning sw=c.getWarnings();
				if(sw!=null) {
					while(sw!=null) {
						getLogger().warn(sw, sw);
						sw=sw.getNextWarning();
					}
					c.clearWarnings();
				}
			}
		} catch(Exception e) {
			getLogger().error("Error checking in DB connection", e);
		}
		// Perform the normal checkIn
		super.checkIn();
	}
}
