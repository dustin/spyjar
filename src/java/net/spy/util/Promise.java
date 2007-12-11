// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

/**
 * A promise, continuation style code for java.
 */
public abstract class Promise<T> extends Object {

	private T rv=null;
	private volatile boolean hasRun=false;
	private RuntimeException bpe=null;

	/**
	 * Get an instance of Promise.
	 */
	public Promise() {
		super();
	}

	/**
	 * Get the object.
	 *
	 * @return the Object we were promised.
	 *
	 * @exception RuntimeException if there's a problem getting what
	 *            we were promised.
	 */
	public final T get() {
		if(hasRun == false) {
			synchronized(this) {
				if(hasRun == false) {
					try {
						rv=execute();
					} catch(RuntimeException e) {
						this.bpe=e;
						throw e;
					} finally {
						hasRun=true;
					}
				}
			}
		}

		// If there was a broken promise, toss it.
		if(bpe!=null) {
			throw bpe;
		}

		return(rv);
	}

	/**
	 * Print me.
	 */
	@Override
	public String toString() {
		String rvs=null;

		if(hasRun) {
			if(rv!=null) {
				rvs="Promise {" + rv.toString() + "}";
			} else {
				if(bpe!=null) {
					rvs="Broken Promise {" + bpe.toString() + "}";
				} else {
					rvs="Promise {null}";
				}
			}
		} else {
			rvs="Promise {not yet executed}";
		}

		return(rvs);
	}

	/**
	 * Do the actual work required to get the Object we're promised.
	 */
	protected abstract T execute();

}
