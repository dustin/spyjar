// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 745EEC35-1110-11D9-82E3-000A957659CC

package net.spy.log;

/**
 * An OutputStream that logs to a Logger.
 */
public class LoggingOutputStream extends LineGettingOutputStream {

	private Logger logger=null;
	private Level level=null;

	private static final String STDOUT_NAME="stdout";
	private static final String STDERR_NAME="stderr";

	/**
	 * Get an instance of LoggingOutputStream.
	 */
	public LoggingOutputStream(String name) {
		this(name, Level.INFO);
	}

	/**
	 * Get an instance of LoggingOutputStream.
	 */
	public LoggingOutputStream(String name, Level lv) {
		super();
		logger=LoggerFactory.getLogger(name);
		this.level=lv;
	}

	/** 
	 * Send the stuff to the logger.
	 * 
	 * @param chunk 
	 */
	protected void processChunk(String chunk) {
		logger.log(level, chunk);
	}

	/** 
	 * Redefine stdout and stderr using new logging streams.
	 */
	public static void redefineOutputs() {
		LoggingOutputStream out=new LoggingOutputStream(STDOUT_NAME);
		LoggingOutputStream err=new LoggingOutputStream(STDERR_NAME,
			Level.ERROR);

		out.setOut();
		err.setErr();
	}

}
