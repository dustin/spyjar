// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: NetStringEncoder.java,v 1.1 2003/05/07 07:45:11 dustin Exp $

package net.spy.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Encode netstrings.
 *
 * @see http://cr.yp.to/proto/netstrings.txt
 */
public class NetStringEncoder extends Object {

	private String encoding=null;

	/**
	 * Get an instance of NetStringEncoder.
	 */
	public NetStringEncoder(String encoding) {
		super();
		this.encoding=encoding;
	}

	/** 
	 * Encode a String as a netstring to the given output stream.
	 * 
	 * @param s the string
	 * @param os the stream
	 * @throws IOException if there's a problem writing to the string
	 */
	public final void encodeString(String s, OutputStream os)
		throws IOException {

		if(s==null) {
			throw new NullPointerException("Cannot encode null string");
		}

		// Write the size
		os.write(String.valueOf(s.length()).getBytes(encoding));
		// Write the colon
		os.write(':');
		// Write the data
		os.write(s.getBytes(encoding));
		// Write the comma
		os.write(',');
	}

}
