// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// $Id: RHash.java,v 1.2 2002/11/20 04:32:06 dustin Exp $

package net.spy;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

import net.spy.rmi.RObject;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

import net.spy.util.NestedException;

/**
 * Abstract client for Remote Hash service.
 */

public class RHash extends SpyObject {

	private String rhashserver=null;
	private RObject obj=null;

	/**
	 * Constructor!
	 *
	 * @param server RMI URL to ObjectServer, for example:
	 * rmi://rmiregistoryserverthing/ObjectServer
	 *
	 * @exception NotBoundException An exception is thrown if an RMI connection
	 * cannot be established.
	 */
	public RHash(String server) throws NestedException {
		super();
		rhashserver = server;
		obj = getobject();
	}

	/**
	 * gets an object from the remote object server.
	 *
	 * @param name name of the object to fetch.
	 *
	 * @return the object from the remote object server.
	 */
	public Object get(String name) {
		Object o;
		try {
			o = obj.getObject(name);
		} catch(RemoteException e) {
			getLogger().warn(
				"Exception while trying to find remote object named "
				+ name, e);
			o = null;
		}
		return(o);
	}

	/**
	 * stores an object in the remote object server.
	 *
	 * @param name key under which to store the object
	 * @param o object to store.
	 */
	public void put(String name, Object o) {
		try {
			obj.storeObject(name, o);
		} catch(RemoteException e) {
			getLogger().warn(
				"Exception while trying to store object named "
				+ name, e);
		}
	}

	/**
	 * Verify we still have a connection to the RMI server.
	 *
	 * @return true if connected
	 */
	public boolean connected() {
		boolean ret=false;
		try {
			ret=obj.ping();
		} catch(RemoteException e) {
			// Doesn't matter
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("Remote object ping failed", e);
			}
		}
		return(ret);
	}

	protected void finalize() throws Throwable {
		obj = null;
		super.finalize();
	}

	private RObject getobject() throws NestedException {
		RObject o = null;
		
		try {
			o=(RObject)Naming.lookup(rhashserver);
		} catch(NotBoundException e) {
			throw new NestedException("Error getting rhash server", e);
		} catch(MalformedURLException e) {
			throw new NestedException("Error getting rhash server", e);
		} catch(RemoteException e) {
			throw new NestedException("Error getting rhash server", e);
		}
		return(o);
	}

	public static void main(String args[]) throws Exception {
		System.out.println("Connecting to local ObjectServer.");
		RHash rh=new RHash("//localhost/RObjectServer");
		System.out.println("Storing something.");
		rh.put("TestString", "Test");
		System.out.println("Waiting ten seconds.");
		Thread.sleep(10000);
		System.out.println("Re-opening ObjectServer connection.");
		rh=new RHash("//localhost/RObjectServer");
		System.out.println("Fetching something.");
		String blah=(String)rh.get("TestString");
		System.out.println("Result:  " + blah);
	}
}
