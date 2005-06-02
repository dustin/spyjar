// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// arch-tag: 725CC440-1110-11D9-93D2-000A957659CC

package net.spy.log;

/**
 * Abstract implementation of Logger providing most of the common
 * framework.
 */
public abstract class AbstractLogger implements Logger {

	private String name=null;

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
		log(Level.DEBUG, message, exception);
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
		log(Level.INFO, message, exception);
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
		log(Level.WARN, message, exception);
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
		log(Level.ERROR, message, exception);
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
		log(Level.FATAL, message, exception);
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
	 * Log a message at the given level.
	 * 
	 * @param level the level
	 * @param message the message
	 */
	public void log(Level level, Object message) {
		log(level, message, null);
	}

	/** 
	 * Subclasses should implement this method to determine what to do when
	 * a client wants to log at a particular level.
	 * 
	 * @param level the level to log at (see the fields of this class)
	 * @param message the message to log
	 * @param e the exception that caused the message (or null)
	 */
	public abstract void log(Level level, Object message, Throwable e);

}
