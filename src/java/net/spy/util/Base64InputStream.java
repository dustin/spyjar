// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 80B3FEF8-1110-11D9-AC13-000A957659CC

package net.spy.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * A filter stream for decoding Base64 data on an InputStream.
 */
public class Base64InputStream extends ByteConverionInputStream {

	private Base64 base64=null;
	// Properly initialized, this will be zero
	private int currentOut=9;
	private byte outbuffer[]=null;

	/**
	 * Get a new Base64InputStream decoding the given InputStream.
	 */
	public Base64InputStream(InputStream is) {
		super(is);
		base64=new Base64();
	}

	/**
	 * Get the next decoded byte in this stream.
	 */
	public int read() throws IOException {
		int rv=-1;

		if(outbuffer==null || currentOut>=outbuffer.length) {
			decodeMore();
		}

		if(outbuffer.length>0) {
			rv=outbuffer[currentOut++];
		}

		return(rv);
	}

	private void decodeMore() throws IOException {
		byte tmp[]=new byte[4];
		boolean more=true;

		int bytesread=0;
		for(bytesread=0; bytesread<4 && more; bytesread++) {
			int input=in.read();
			if(input<0) {
				more=false;
			} else {
				if(base64.isValidBase64Char( (char)input) ) {
					tmp[bytesread]=(byte)input;
				} else {
					// Skip this byte 
					bytesread--;
				} // Deal with the read character
			} // Got input
		} // Getting input

		String todecode=null;

		if(bytesread<4) {
			byte tmptmp[]=new byte[bytesread];
			System.arraycopy(tmp, 0, tmptmp, 0, bytesread);
			todecode=new String(tmptmp);
		} else {
			todecode=new String(tmp);
		}
		outbuffer=base64.decode(todecode);

		currentOut=0;
	}

	/**
	 * Return the number of bytes that may be read without blocking.
	 * This is kind of a guess based on the number of bytes available.  It
	 * probably works.
	 */
	public int available() throws IOException {
		int rv=in.available();
		rv=(rv*3)/4;
		return(rv);
	}

}
