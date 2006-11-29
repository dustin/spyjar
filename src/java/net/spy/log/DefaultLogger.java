// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// arch-tag: 7296FF66-1110-11D9-8D65-000A957659CC

package net.spy.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Default logger implementation.
 *
 * This logger is really primitive.  It just logs everything to stderr if
 * it's higher than INFO.
 */
public class DefaultLogger extends AbstractLogger {

	private SimpleDateFormat df=null;

	/**
	 * Get an instance of DefaultLogger.
	 */
	public DefaultLogger(String name) {
		super(name);
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}

	/** 
	 * False.
	 */
	@Override
	public boolean isDebugEnabled() {
		return(false);
	}

	/** 
	 * True.
	 */
	@Override
	public boolean isInfoEnabled() {
		return(true);
	}

	/** 
	 * @see AbstractLogger
	 */
	@Override
	public synchronized void log(Level level, Object message, Throwable e) {
		if(level == Level.INFO
			|| level == Level.WARN
			|| level == Level.ERROR
			|| level == Level.FATAL) {
			System.err.printf("%s %s %s:  %s\n",
					df.format(new Date()), level.name(), getName(), message);
			if(e != null) {
				e.printStackTrace();
			}
		}
	}

}
