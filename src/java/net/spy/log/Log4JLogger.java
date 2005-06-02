// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: CB638855-1110-11D9-A098-000A957659CC

package net.spy.log;

/**
 * Logging implementation using
 * <a href="http://jakarta.apache.org/log4j/docs/">log4j</a>.
 */
public class Log4JLogger extends AbstractLogger {

	// Can't really import this without confusion as there's another thing
	// by this name in here.
	private org.apache.log4j.Logger l4jLogger=null;

	/**
	 * Get an instance of Log4JLogger.
	 */
	public Log4JLogger(String name) {
		super(name);

		// Get the log4j logger instance.
		l4jLogger=org.apache.log4j.Logger.getLogger(name);
	}

	/** 
	 * True if the underlying logger would allow debug messages through.
	 */
	public boolean isDebugEnabled() {
		return(l4jLogger.isDebugEnabled());
	}

	/** 
	 * True if the underlying logger would allow info messages through.
	 */
	public boolean isInfoEnabled() {
		return(l4jLogger.isInfoEnabled());
	}

	/** 
	 * Wrapper around log4j.
	 * 
	 * @param level net.spy.log.AbstractLogger level.
	 * @param message object message
	 * @param e optional throwable
	 */
	public void log(Level level, Object message, Throwable e) {
		org.apache.log4j.Level pLevel=org.apache.log4j.Level.DEBUG;

		if(level==Level.DEBUG) {
			pLevel=org.apache.log4j.Level.DEBUG;
		} else if(level==Level.INFO) {
			pLevel=org.apache.log4j.Level.INFO;
		} else if(level==Level.WARN) {
			pLevel=org.apache.log4j.Level.WARN;
		} else if(level==Level.ERROR) {
			pLevel=org.apache.log4j.Level.ERROR;
		} else if(level==Level.FATAL) {
			pLevel=org.apache.log4j.Level.FATAL;
		} else {
			// XXX:  Need to do something here.
		}

		l4jLogger.log("net.spy.log.AbstractLogger", pLevel, message, e);
	}

}
