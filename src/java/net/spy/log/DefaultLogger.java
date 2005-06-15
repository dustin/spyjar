// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// arch-tag: 7296FF66-1110-11D9-8D65-000A957659CC

package net.spy.log;

/**
 * Default logger implementation.
 *
 * This logger is really primitive.  It just logs everything to stderr if
 * it's higher than INFO.
 */
public class DefaultLogger extends AbstractLogger {

	/**
	 * Get an instance of DefaultLogger.
	 */
	public DefaultLogger(String name) {
		super(name);
	}

	/** 
	 * False.
	 */
	public boolean isDebugEnabled() {
		return(false);
	}

	/** 
	 * True.
	 */
	public boolean isInfoEnabled() {
		return(true);
	}

	/** 
	 * @see AbstractLogger
	 */
	public void log(Level level, Object message, Throwable e) {
		if(level == Level.INFO
			|| level == Level.WARN
			|| level == Level.ERROR
			|| level == Level.FATAL) {
			System.err.println(getName() + " (" + level.getName()
				+ "): " + message);
			if(e != null) {
				e.printStackTrace();
			}
		}
	}

}
