// Copyright (c) 2000  Dustin Sallings
//
// arch-tag: 708DB13D-1110-11D9-8D9F-000A957659CC

package net.spy.info;

/**
 * Abstract class for Info classes that describe shipping information.
 */
public abstract class PackageInfo extends Info {

	private boolean delivered=false;

	/**
	 * True if the package has been delivered.
	 */
	public boolean isDelivered() {
		return(delivered);
	}

	/**
	 * Set the delivered status.
	 */
	protected void setDelivered() {
		delivered=true;
	}
}
