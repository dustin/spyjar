// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: SavableNode.java,v 1.1 2003/01/15 08:08:06 dustin Exp $

package net.spy.db;

import java.util.Collection;

/**
 * Interface for transactionally savable trees of objects.
 *
 * This interface supersedes Savable.
 */
public interface SavableNode extends Savable {

	/** 
	 * Get a Collection of all of the SavableNodes this SavableNode is
	 * holding that will need to be saved before this Savable.
	 * 
	 * @param context SaveContext being used in this Saver session
	 * @return a collection of objects this SavableNode depends on
	 */
	Collection getPreSavables(SaveContext context);

	/** 
	 * Get a Collection of all of the SavableNodes this SavableNode is
	 * holding that will need to be saved after this Savable.
	 * 
	 * @param context SaveContext being used in this Saver session
	 * @return a collection of objects this SavableNode depends on
	 */
	Collection getPostSavables(SaveContext context);

	/** 
	 * This method is deprecated and should return the same value as
	 * getPostSavables.
	 */
	Collection getSavables(SaveContext context);

}
