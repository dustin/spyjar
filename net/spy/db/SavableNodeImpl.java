// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: SavableNodeImpl.java,v 1.1 2003/09/05 07:32:30 dustin Exp $

package net.spy.db;

import java.util.Collection;

import net.spy.SpyObject;

/**
 * Abstract {@link SavableNode} implementation to be used a superclass for
 * savable objects where applicable.
 */
public class SavableNodeImpl extends SpyObject {

	private boolean sniIsNew=false;
	private boolean sniIsModified=false;

	private Exception createdHere=null;

	/**
	 * Get an instance of SavableNodeImpl.
	 *
	 * This constructor sets the <q>new</q> flag.
	 */
	public SavableNodeImpl() {
		super();
		createdHere=new UnsavedObjectWarning("CREATED HERE");
		createdHere.fillInStackTrace();
		sniIsNew=true;
	}

	/** 
	 * Indicate whether this object is new.
	 */
	public boolean isNew() {
		return(sniIsNew);
	}

	/** 
	 * Indicate whether this object is modified.
	 */
	public boolean isModified() {
		return(sniIsModified);
	}

	/** 
	 * Set the new flag.
	 */
	protected void setNew(boolean to) {
		sniIsNew=to;
	}

	/** 
	 * Set the modified flag.
	 */
	protected void setModified(boolean to) {
		sniIsModified=to;
	}

	/** 
	 * @see SavableNode
	 *
	 * @return null - no objects depending on this instance
	 */
	public Collection getSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * @see SavableNode
	 * 
	 * @return null - no objects this object depends on
	 */
	public Collection getPreSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * @see Savable
	 *
	 * @return  null - no objects depending on this instance
	 */
	public Collection getPostSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * Mark this object as saved.
	 */
	protected void setSaved() {
		setNew(false);
		setModified(false);
	}

	/** 
	 * If this object is still modified or new at finalization time, log
	 * the exception.
	 */
	protected void finalize() throws Throwable {
		if(isNew() || isModified()) {
			getLogger().debug("Finalizing an object that needs saved",
				createdHere);
		}
	}

	/** 
	 * Exception logged when an object is finalized before modification.
	 */
	private static final class UnsavedObjectWarning extends Exception {
		public UnsavedObjectWarning(String msg) {
			super(msg);
		}
	}

}
