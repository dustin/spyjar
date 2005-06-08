// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: F94DC345-7A9E-4B23-B35F-1F0E64CAA360

package net.spy.test;

import junit.framework.TestCase;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;
import net.spy.log.SunLogger;

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
		l.log(null, "test null", null);
	}

}
