// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: Level.java,v 1.1 2002/11/20 06:20:01 dustin Exp $

package net.spy.log;

/**
 * Levels for logging.
 */
public final class Level extends Object {

	/** 
	 * Debug level.
	 */
	public static final Level DEBUG=new Level("DEBUG");
	/** 
	 * Info level.
	 */
	public static final Level INFO=new Level("INFO");
	/** 
	 * Warning level.
	 */
	public static final Level WARN=new Level("WARN");
	/** 
	 * Error level.
	 */
	public static final Level ERROR=new Level("ERROR");
	/** 
	 * Fatal level.
	 */
	public static final Level FATAL=new Level("FATAL");

	private String name=null;

	private Level(String name) {
		super();
		this.name=name;
	}

	/** 
	 * Get the name of this level.
	 */
	public String getName() {
		return(name);
	}

	/** 
	 * Get a string representation of this level.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(16);

		sb.append("{LogLevel:  ");
		sb.append(name);
		sb.append("}");

		return(sb.toString());
	}

}
