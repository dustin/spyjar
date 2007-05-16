// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 84758527-1110-11D9-B558-000A957659CC

package net.spy.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Encode netstrings.
 *
 * See <a href="http://cr.yp.to/proto/netstrings.txt">netstring spec</a>.
 */
public class NetStringEncoder extends Object {

	private final String encoding;

	/**
	 * Get an instance of NetStringEncoder.
	 */
	public NetStringEncoder(String enc) {
		super();
		encoding=enc;
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
