// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// $Id: LoggerFactory.java,v 1.2 2002/11/05 04:59:09 dustin Exp $

package net.spy.log;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.spy.util.NestedRuntimeException;

/**
 * Factory to get logger instances.
 * The system property <code>net.spy.log.LoggerImpl</code> should point to
 * an implementation of net.spy.log.Logger to use.
 */
public class LoggerFactory extends Object {

	private Map instances=null;
	private Constructor instanceConstructor=null;
	private boolean consTakesName=true;

	private static LoggerFactory instance=null;

	/**
	 * Get an instance of LoggerFactory.
	 */
	private LoggerFactory() {
		super();

		instances=Collections.synchronizedMap(new HashMap());
	}

	private static synchronized void init() {
		if(instance==null) {
			instance=new LoggerFactory();
		}
	}

	/** 
	 * Get a logger by class.
	 * 
	 * @param clazz the class for which we want the logger.
	 * @return a Logger instance
	 */
	public static Logger getLogger(Class clazz) {
		return(getLogger(clazz.getName()));
	}

	/** 
	 * Get a logger by name.
	 * 
	 * @param name the name for which we want the logger
	 * @return a Logger instance
	 */
	public static Logger getLogger(String name) {
		init();
		return(instance.internalGetLogger(name));
	}

	// Get an instance of Logger from internal mechanisms.
	private Logger internalGetLogger(String name) {
		Logger rv=(Logger)instances.get(name);

		if (rv==null) {
			try {
				rv=getNewInstance(name);
			} catch(Exception e) {
				throw new NestedRuntimeException("Problem getting logger", e);
			}
			// Remember it for later
			instances.put(name, rv);
		}

		return(rv);
	}

	private Logger getNewInstance(String name)
		throws	InstantiationException, IllegalAccessException,
				IllegalArgumentException, InvocationTargetException {

		if(instanceConstructor==null) {
			getConstructor();
		}
		Object args[]={name};
		Logger rv=(Logger)instanceConstructor.newInstance(args);

		return (rv);
	}

	// Find the appropriate constructor
	private void getConstructor() {
		Class c=DefaultLogger.class;
		String className=System.getProperty("net.spy.log.LoggerImpl");

		if(className!=null) {
			try {
				c=Class.forName(className);
			} catch(ClassNotFoundException e) {
				System.err.println("Warning:  " + className
					+ " not found while initializing"
					+ " net.spy.log.LoggerFactory");
				c=DefaultLogger.class;
			}
		}

		// Find the best constructor
		try {
			// Try to find a constructor that takes a single string
			Class args[]={String.class};
			instanceConstructor=c.getConstructor(args);
		} catch(NoSuchMethodException e) {
			try {
				// Try to find an empty constructor
				Class args[]={};
				instanceConstructor=c.getConstructor(args);
			} catch(NoSuchMethodException e2) {
				System.err.println("Warning:  " + className +
					" has no appropriate constructor, switching to defaults.");

				// Try to find a constructor that takes a single string
				try {
					Class args[]={String.class};
					instanceConstructor=
						DefaultLogger.class.getConstructor(args);
				} catch(NoSuchMethodException e3) {
					// This shouldn't happen.
					throw new NoSuchMethodError(
						"There used to be a constructor that takes a single "
							+ "String on "
							+ DefaultLogger.class + ", but I can't "
							+ "find one now.");
				} // SOL
			} // No empty constructor
		} // No constructor that takes a string
	} // getConstructor

}
