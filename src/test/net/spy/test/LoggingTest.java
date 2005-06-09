// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: F94DC345-7A9E-4B23-B35F-1F0E64CAA360

package net.spy.test;

import java.io.PrintStream;

import junit.framework.TestCase;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;
import net.spy.log.SunLogger;
import net.spy.log.Syslog;
import net.spy.log.LoggingOutputStream;

/**
 * Make sure logging is enabled.
 */
public class LoggingTest extends TestCase {

	private Logger logger=null;

	/**
	 * Get an instance of LoggingTest.
	 */
	public LoggingTest(String name) {
		super(name);
	}

	/** 
	 * Set up logging.
	 */
	public void setUp() {
		logger=LoggerFactory.getLogger(getClass());
	}

	/** 
	 * Make sure logging is enabled.
	 */
	public void testDebugLogging() {
		assertTrue(logger.isDebugEnabled());
		logger.debug("debug message");
	}

	/** 
	 * Make sure info is enabled, and test it.
	 */
	public void testInfoLogging() {
		assertTrue(logger.isInfoEnabled());
		logger.info("info message");
	}

	/** 
	 * Test other log stuff.
	 */
	public void testOtherLogging() {
		logger.warn("warn message");
		logger.error("error message");
		logger.fatal("fatal message");
		logger.log(null, "test null", null);
	}

	/** 
	 * Make sure we're using log4j.
	 */
	public void testLog4j() {
		Logger logger=LoggerFactory.getLogger(getClass());
		assertEquals("net.spy.log.Log4JLogger", logger.getClass().getName());
	}

	/** 
	 * Test the sun logger.
	 */
	public void testSunLogger() {
		Logger l=new SunLogger(getClass().getName());
		assertFalse(l.isDebugEnabled());
		l.debug("debug message");
		assertTrue(l.isInfoEnabled());
		l.info("info message");
		l.warn("warn message");
		l.error("error message");
		l.fatal("fatal message");
		l.fatal("fatal message with exception", new Exception());
		l.log(null, "test null", null);
		l.log(null, "null message with exception and no requestor",
			new Exception());
	}

	/** 
	 * Test syslog.  This test just pretty much makes coverage and stuff.
	 */
	public void testSyslog() throws Exception {
		Syslog syslog=new Syslog("localhost");
		syslog.log(Syslog.LOCAL0, Syslog.DEBUG, "Test via syslog");
	}

	/** 
	 * Test the logging output streams.
	 * 
	 * @throws Exception 
	 */
	public void testLoggingStreams() throws Exception {
		PrintStream outSave=System.out;
		PrintStream errSave=System.err;
		try {
			LoggingOutputStream.redefineOutputs();
			System.out.println("logging via stdout");
			System.out.println("logging via stdout\nWith multiple lines");
			System.err.println("logging via stderr");
			System.out.write('b');
			System.out.write('y');
			System.out.write('t');
			System.out.write('e');
			System.out.write('\r');
			System.out.write('\n');
			System.out.write('t');
			System.out.write('e');
			System.out.write('s');
			System.out.write('t');
			System.out.write('\r');
			System.out.write('\n');

			String testString=" byte\rarray\ntest\r\n";
			System.out.write(testString.getBytes(), 1, testString.length()-1);
		} finally {
			System.setOut(outSave);
			System.setErr(errSave);
		}
	}

}
