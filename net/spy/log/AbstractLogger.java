// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// $Id: AbstractLogger.java,v 1.2 2002/11/05 06:06:52 dustin Exp $

package net.spy.log;

/**
 * Abstract implementation of Logger providing most of the common
 * framework.
 */
public abstract class AbstractLogger implements Logger {

	/** 
	 * Value representing the abstract debug level.
	 */
	protected final static int DEBUG=1;
	/** 
	 * Value representing the abstract info level.
	 */
	protected final static int INFO=2;
	/** 
	 * Value representing the abstract warning level.
	 */
	protected final static int WARN=3;
	/** 
	 * Value representing the abstract error level.
	 */
	protected final static int ERROR=4;
	/** 
	 * Value representing the abstract fatal level.
	 */
	protected final static int FATAL=5;

	private static String name=null;

	/** 
	 * Instantiate the abstract logger.
	 */
	protected AbstractLogger(String name) {
		super();
		if(name == null) {
			throw new NullPointerException("Logger name may not be null.");
		}
		this.name=name;
	}

	/** 
	 * Get the String value of a particular log level.
	 * 
	 * @param level the log level to get
	 * @return the name of that log level
	 */
	protected String levelToString(int level) {
		String rv=null;
		switch(level) {
			case DEBUG:
				rv="DEBUG";
				break;
			case INFO:
				rv="INFO";
				break;
			case WARN:
				rv="WARN";
				break;
			case ERROR:
				rv="ERROR";
				break;
			case FATAL:
				rv="FATAL";
				break;
			default:
				throw new IllegalArgumentException(level
					+ " is an invalid log type.");
		}
		return (rv);
	}

	/** 
	 * Get the name of this logger.
	 */
	public String getName() {
		return(name);
	}

	/** 
	 * True if debug is enabled for this logger.
	 * Default implementation always returns false
	 * 
	 * @return true if debug messages would be displayed
	 */
	public boolean isDebugEnabled() {
		return(false);
	}

	/** 
	 * True if debug is enabled for this logger.
	 * Default implementation always returns false
	 * 
	 * @return true if info messages would be displayed
	 */
	public boolean isInfoEnabled() {
		return(false);
	}

	/** 
	 * Log a message at debug level.
	 * 
	 * @param message the message to log
	 * @param exception the exception that caused the message to be generated
	 */
	public void debug(Object message, Throwable exception) {
		logAt(DEBUG, message, exception);
	}
	/** 
	 * Log a message at debug level.
	 * 
	 * @param message the message to log
	 */
	public void debug(Object message) {
		debug(message, null);
	}

	/** 
	 * Log a message at info level.
	 * 
	 * @param message the message to log
	 * @param exception the exception that caused the message to be generated
	 */
	public void info(Object message, Throwable exception) {
		logAt(INFO, message, exception);
	}
	/** 
	 * Log a message at info level.
	 * 
	 * @param message the message to log
	 */
	public void info(Object message) {
		info(message, null);
	}

	/** 
	 * Log a message at warning level.
	 * 
	 * @param message the message to log
	 * @param exception the exception that caused the message to be generated
	 */
	public void warn(Object message, Throwable exception) {
		logAt(WARN, message, exception);
	}
	/** 
	 * Log a message at warning level.
	 * 
	 * @param message the message to log
	 */
	public void warn(Object message) {
		warn(message, null);
	}

	/** 
	 * Log a message at error level.
	 * 
	 * @param message the message to log
	 * @param exception the exception that caused the message to be generated
	 */
	public void error(Object message, Throwable exception) {
		logAt(ERROR, message, exception);
	}
	/** 
	 * Log a message at error level.
	 * 
	 * @param message the message to log
	 */
	public void error(Object message) {
		error(message, null);
	}

	/** 
	 * Log a message at fatal level.
	 * 
	 * @param message the message to log
	 * @param exception the exception that caused the message to be generated
	 */
	public void fatal(Object message, Throwable exception) {
		logAt(FATAL, message, exception);
	}

	/** 
	 * Log a message at fatal level.
	 * 
	 * @param message the message to log
	 */
	public void fatal(Object message) {
		fatal(message, null);
	}

	/** 
	 * Subclasses should implement this method to determine what to do when
	 * a client wants to log at a particular level.
	 * 
	 * @param level the level to log at (see the fields of this class)
	 * @param message the message to log
	 * @param e the exception that caused the message (or null)
	 */
	protected abstract void logAt(int level, Object message, Throwable e);

}
