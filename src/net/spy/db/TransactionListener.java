// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6EED00D5-1110-11D9-8892-000A957659CC

package net.spy.db;

/**
 * Interface for objects that want to be notified when they're at the end
 * of their transaction.  Any Savable that implements this interface will
 * receive this notification.
 */
public interface TransactionListener {

	/** 
	 * Method called when the transaction is over.
	 */
	void transactionCommited();

}
