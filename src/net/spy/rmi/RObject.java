// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// arch-tag: 7F642423-1110-11D9-BBF3-000A957659CC

package net.spy.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RObject is an RMI service that allows you to access a hash table stored
 * on a remote machine's disk.
 */

public interface RObject extends Remote { 
	/**
	 * Store an Object in the object server.
	 *
	 * @param name Key under which the object will be stored
	 * @param o    Object to store
	 *
	 * @throws RemoteException when something breaks
	 */
    void storeObject(String name, Object o) throws RemoteException; 

	/**
	 * Fetch an object from the object server.
	 *
	 * @param name Name of the object to fetch.
	 *
	 * @return the object
	 *
	 * @throws RemoteException when something breaks
	 */
    Object getObject(String name) throws RemoteException; 

	/**
	 * Make sure the server is available and functioning.
	 *
	 * @return true if the server is awake.
	 *
	 * @throws RemoteException when something breaks
	 */
    boolean ping() throws RemoteException; 
}
