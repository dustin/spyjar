// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: AbstractSavable.java,v 1.3 2003/08/05 09:01:03 dustin Exp $

package net.spy.db;

import java.util.Collection;

import net.spy.SpyObject;

/**
 * Abstract implementation of {@link SavableNode}.
 */
public abstract class AbstractSavable extends SpyObject
	implements SavableNode {

	private boolean asIsNew=false;
	private boolean asIsModified=false;

	/**
	 * Get an instance of AbstractSavable.
	 *
	 * An object instantiated with this constructor will have the new flag
	 * set.  If this is not desirable, unset it.
	 */
	protected AbstractSavable() {
		super();
		asIsNew=true;
	}

	/** 
	 * Indicate whether this object is new.
	 */
	public boolean isNew() {
		return(asIsNew);
	}

	/** 
	 * Indicate whether this object has been modified.
	 */
	public boolean isModified() {
		return(asIsModified);
	}

	/** 
	 * Set the ``new'' flag for this object.
	 * 
	 * @param to the new value of the new flag
	 */
	protected void setNew(boolean to) {
		asIsNew=to;
	}

	/** 
	 * Set the ``modified'' flag for this object.
	 * 
	 * @param to the new value for the modified flag
	 */
	protected void setModified(boolean to) {
		asIsModified=to;
	}

	/** 
	 * Get the dependent objects for this Savable.  The default
	 * implementation returns null, indicating that there are no dependent
	 * objects.
	 */
	public Collection getSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * Get the objects that need to be saved before this object.
	 * The default implementation returns null, indicating that there are
	 * no prerequisite objects.
	 */
	public Collection getPreSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * Get the dependent objects for this Savable. The default
	 * implementation returns null, indicating that there are no dependent
	 * objects.
	 */
	public Collection getPostSavables(SaveContext context) {
		return(null);
	}

	/** 
	 * Unset the flags indicating that this object needs to be saved.
	 */
	protected void setSaved() {
		setNew(false);
		setModified(false);
	}

}
