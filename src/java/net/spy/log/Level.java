// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 72D7F78E-1110-11D9-B5E5-000A957659CC

package net.spy.log;

/**
 * Levels for logging.
 */
public enum Level {

	/** 
	 * Debug level.
	 */
	DEBUG,
	/** 
	 * Info level.
	 */
	INFO,
	/** 
	 * Warning level.
	 */
	WARN,
	/** 
	 * Error level.
	 */
	ERROR,
	/** 
	 * Fatal level.
	 */
	FATAL;

	/** 
	 * Get a string representation of this level.
	 */
	public String toString() {
		return("{LogLevel:  " + name() + "}");
	}

}
