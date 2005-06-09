// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 734198D6-1110-11D9-B33B-000A957659CC

package net.spy.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * An output stream that processes a line at a time.
 */
public abstract class LineGettingOutputStream extends OutputStream {

	/**
	 * Get an instance of LineGettingOutputStream.
	 */
	public LineGettingOutputStream() {
		super();
	}

	/**
	 * Write a byte.
	 */
	public void write(int b) throws IOException {
		byte ba[]=new byte[1];
		ba[0]=(byte)b;
		write(ba, 0, 1);
	}

	/**
	 * Do the actual writing.
	 */
	public void write(byte b[], int offset, int length) throws IOException {
		// Make a string and get rid of the extra space at the ends.
		String msgS=new String(b, offset, length).trim();
		if(msgS.length() > 0 ) {
			processChunk(msgS);
		}
	}
	
	/** 
	 * Deal with a chunk of data.
	 * 
	 * @param chunk the chunk of data to process
	 * @throws IOException if there's a problem processing it
	 */
	protected abstract void processChunk(String chunk) throws IOException;

	/**
	 * Redefine stderr to send messages via this thing.
	 */
	public void setErr() {
		System.setErr(new PrintStream(this));
	}

	/**
	 * Redefine stdout to send messages via this thing.
	 */
	public void setOut() {
		System.setOut(new PrintStream(this));
	}

}
