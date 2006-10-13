// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6B3A933A-1110-11D9-A201-000A957659CC

package net.spy.db;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Store a range of primary keys.
 */
public class KeyStore extends Object {

	private BigDecimal start=null;
	private BigDecimal end=null;
	private AtomicReference<BigDecimal> current=null;

	private static final BigDecimal ONE=new BigDecimal(1);

	/**
	 * Get an instance of KeyStore.
	 */
	public KeyStore(BigDecimal s, BigDecimal e) {
		super();
		this.start=s;
		this.current=new AtomicReference<BigDecimal>(s);
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
	public BigDecimal nextKey() throws OverDrawnException {
		boolean found=false;
		BigDecimal rv=null;
		// keep cycling until we're overdrawn or the atomic is happy
		while(!found) {
			rv=current.get();
			// Make sure we don't run out
			if(rv.compareTo(end) > 0) {
				throw new OverDrawnException();
			}
			// increment
			found=current.compareAndSet(rv, rv.add(ONE));
		}
		return(rv);
	}

}
