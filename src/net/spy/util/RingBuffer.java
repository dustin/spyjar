// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 86FFCE4F-1110-11D9-A139-000A957659CC

package net.spy.util;

import java.io.Serializable;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;

/**
 * A circular buffer.  Ring buffers may have new entries appended to them,
 * but may not otherwise be modified.  Individual entries may not be
 * accessed directly, only via the iterator.
 */
public class RingBuffer extends AbstractCollection implements Serializable {

	private Object buf[]=null;
	private int start=0;
	private int end=0;
	private boolean wrapped=false;
	private int size=0;

	private static final long serialVersionUID=823830283278235L;

	/**
	 * Get an instance of RingBuffer.
	 */
	public RingBuffer(int s) {
		super();
		buf=new Object[s];
		Arrays.fill(buf, null);
	}

	/** 
	 * Get a RingBuffer at a particular size filled from the given
	 * Collection.
	 * 
	 * @param s the maximum number of objects to be held in the ring
	 * @param fill a Collection whose elements will be used to fill the buffer
	 */
	public RingBuffer(int s, Collection fill) {
		this(s);
		for(Iterator i=fill.iterator(); i.hasNext();) {
			add(i.next());
		}
	}

	/** 
	 * Add a new object to the ring.
	 *
	 * @deprecated use add()
	 */
	public void addObject(Object o) {
		add(o);
	}

	/**
	 * Add an object to the ring buffer (if it's full, it'll cycle the
	 * oldest one out).
	 *
	 * @param o the object to add
	 * @return true
	 */
	public boolean add(Object o) {
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
	public String toString() {
		StringBuffer sb=new StringBuffer(256);
		sb.append("{RingBuffer cap=");
		sb.append(getCapacity());
		sb.append(" s=");
		sb.append(start);
		sb.append(", e=");
		sb.append(end);
		sb.append(" [");
		for(int i=0; i<buf.length; i++) {
			sb.append(buf[i]);
			sb.append(" ");
		}
		sb.append("]\n\t");
		ArrayList a=new ArrayList(this);
		sb.append(a.toString());
		sb.append("}");
		return(sb.toString());
	}

	/**
	 * Get the number of objects in this RingBuffer.
	 *
	 * @deprecated use the Collections implementation instead
	 */
	public int getSize() {
		return(size);
	}

	/**
	 * Get the number of objects in this RingBuffer.
	 */
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
	public Iterator iterator() {
		return (new RingBufferIterator());
	}

	/** 
	 * Get the collection of data in this list.
	 * @deprecated this is a Collection now.
	 */
	public Collection getData() {
		return(this);
	}

	// Iterator implementation

	private class RingBufferIterator extends Object
		implements Iterator, Serializable {

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

		public Object next() {
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
