// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 844746A2-1110-11D9-AFA2-000A957659CC

package net.spy.util;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;

import net.spy.SpyObject;

/**
 * Decode netstrings.
 *
 * See <a href="http://cr.yp.to/proto/netstrings.txt">netstring spec</a>.
 */
public class NetStringDecoder extends SpyObject {

	// Maximum length of the length
	private static final int MAX_LEN_LEN=6;
	// Maximum length of the string
	private static final int MAX_LEN=65535;

	private String encoding=null;

	/**
	 * Get an instance of NetStringDecoder.
	 */
	public NetStringDecoder(String encoding) {
		super();
		this.encoding=encoding;
	}

	/** 
	 * Pull a netstring from the given InputStream and decode it.
	 * 
	 * @param is a stream containing a netstring
	 * @return the String representing the netstring at the beginning of
	 * 			this stream
	 * @throws IOException if there is a problem decoding the netstring
	 */
	public String decodeString(InputStream is) throws IOException {
		StringBuffer sizeBuf=new StringBuffer(10);
		boolean haveSize=false;
		while(!haveSize) {
			int c=is.read();
			if(c==-1) {
				throw new EOFException("End of stream reading size");
			}
			// Check for a size character
			if(c == '0' || c == '1' || c == '2' || c == '3' || c == '4'
				|| c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {

				sizeBuf.append( (char)c);
				if(sizeBuf.length() > MAX_LEN_LEN) {
					throw new IOException(
						"Length of netstring length too long:  " + sizeBuf);
				}
			} else if(c == ':') {
				haveSize = true;
			} else {
				throw new IOException("Illegal character in netstring size:  "
					+ (char)c);
			}
		} // Getting the size

		// Get the size
		int size=Integer.parseInt(sizeBuf.toString());
		if(size > MAX_LEN) {
			throw new IOException("Netstring too long:  " + size);
		}

		// Read enough data
		byte tmp[]=new byte[size];
		int bytesread=0;
		while(bytesread < size) {
			int r=is.read(tmp, bytesread, (size-bytesread));
			if(r == -1) {
				throw new EOFException("EOF waiting for netstring");
			}
			bytesread+=r;
		}
		int c=is.read();
		if(c != ',') {
			throw new IOException("Expected comma");
		}

		// Stringify the data
		String rv=new String(tmp, encoding);

		return(rv);
	}

}
