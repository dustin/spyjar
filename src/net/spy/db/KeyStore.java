// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6B3A933A-1110-11D9-A201-000A957659CC

package net.spy.db;

import java.math.BigDecimal;

/**
 * Store a range of primary keys.
 */
public class KeyStore extends Object {

	private BigDecimal start=null;
	private BigDecimal end=null;
	private BigDecimal current=null;

	private static final BigDecimal ONE=new BigDecimal(1);

	/**
	 * Get an instance of KeyStore.
	 */
	public KeyStore(BigDecimal s, BigDecimal e) {
		super();
		this.start=s;
		this.current=s;
		this.end=e;
	}

	/** 
	 * String me.
	 */
	public String toString() {
		return("KeyStore from " + start + " to " + end);
	}

	/** 
	 * Get the next key.
	 * 
	 * @return the next available key
	 * @throws OverDrawnException if there are no keys left in this store
	 */
	public synchronized BigDecimal nextKey() throws OverDrawnException {
		BigDecimal rv=current;
		// Make sure we don't run out
		if(current.compareTo(end) > 0) {
			throw new OverDrawnException();
		}
		// increment
		current=current.add(ONE);
		return(rv);
	}

}
