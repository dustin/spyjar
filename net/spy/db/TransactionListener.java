// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: TransactionListener.java,v 1.1 2003/01/07 07:04:21 dustin Exp $

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
