// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.sql.SQLException;

/**
 * Exception thrown when there's a database initialization problem.
 */
public class DBInitException extends SQLException {

	/**
	 * Get an instance of DBInitException.
	 */
	public DBInitException() {
		super();
	}

	/**
	 * Get an instance of DBInitException with a message.
	 */
	public DBInitException(String msg) {
		super(msg);
	}

}
