// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A circular buffer.  Ring buffers may have new entries appended to them,
 * but may not otherwise be modified.  Individual entries may not be
 * accessed directly, only via the iterator.
 */
public class RingBuffer<T> extends AbstractCollection<T>
	implements Serializable {

	final T[] buf;
	int start=0;
	int end=0;
	private boolean wrapped=false;
	private int size=0;

	private static final long serialVersionUID=823830283278235L;

	/**
	 * Get an instance of RingBuffer.
	 */
	@SuppressWarnings("unchecked")
	public RingBuffer(int s) {
		super();
		buf=(T[])new Object[s];
		Arrays.fill(buf, null);
	}

	/** 
	 * Get a RingBuffer at a particular size filled from the given
	 * Collection.
	 * 
	 * @param s the maximum number of objects to be held in the ring
	 * @param fill a Collection whose elements will be used to fill the buffer
	 */
	public RingBuffer(int s, Collection<? extends T> fill) {
		this(s);
		for(T ob : fill) {
			add(ob);
		}
	}

	/**
	 * Add an object to the ring buffer (if it's full, it'll cycle the
	 * oldest one out).
	 *
	 * @param o the object to add
	 * @return true
	 */
	@Override
	public boolean add(T o) {
		if(end>=buf.length) {
			// Will get set to 0
			end=0;
			wrapped=true;
		}
		buf[end]=o;
		end++;
		if(wrapped) {
			start++;
			if(start>=buf.length) {
				start=0;
			}
		} else {
			size++;
		}

		return (true);
	}

	/** 
	 * Check to see if the ring buffer has wrapped.
	 * 
	 * @return true if the ring buffer has wrapped
	 */
	public boolean hasWrapped() {
		return(wrapped);
	}

	/**
	 * String me.
	 */
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(256);
		sb.append("{RingBuffer cap=");
		sb.append(getCapacity());
		sb.append(" s=");
		sb.append(start);
		sb.append(", e=");
		sb.append(end);
		sb.append(" [");
		for(T ob : buf) {
			sb.append(ob);
			sb.append(" ");
		}
		sb.append("]\n\t");
		ArrayList<T> a=new ArrayList<T>(this);
		sb.append(a.toString());
		sb.append("}");
		return(sb.toString());
	}

	/**
	 * Get the number of objects in this RingBuffer.
	 */
	@Override
	public int size() {
		return(size);
	}

	/**
	 * Get the total capacity of this RingBuffer.
	 * @return the number of objects this RingBuffer will hold
	 */
	public int getCapacity() {
		return(buf.length);
	}

	/** 
	 * Get the iterator for this ring buffer.
	 * 
	 * @return an iterator
	 */
	@Override
	public Iterator<T> iterator() {
		return (new RingBufferIterator());
	}

	// Iterator implementation

	class RingBufferIterator extends Object
		implements Iterator<T>, Serializable {

		private int pos=0;
		private int startPos=0;
		private int remaining=0;

		public RingBufferIterator() {
			super();
			pos=start;
			startPos=start;
			remaining=size();
		}

		public boolean hasNext() {
			if(start != startPos) {
				throw new ConcurrentModificationException(
					"Looks like additions have been made to the "
						+ "RingBuffer since the creation of this iterator.");
			}
			return(remaining > 0);
		}

		public T next() {
			if(!hasNext()) {
				throw new NoSuchElementException("Your buffer runneth under.");
			}
			remaining--;
			if(pos==buf.length) {
				pos=0;
			}
			return(buf[pos++]);
		}

		public void remove() {
			throw new UnsupportedOperationException("Nope.");
		}

	}

}
