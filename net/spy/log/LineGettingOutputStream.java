// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: LineGettingOutputStream.java,v 1.1 2002/11/20 06:20:01 dustin Exp $

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
		write(ba, 0, 0);
	}

	/**
	 * Do the actual writing.
	 */
	public void write(byte b[], int offset, int length) throws IOException {
		// Make a string and get rid of the extra space at the ends.
		// String msgS=new String(b, offset, length).trim();
		while(length>=offset && ( b[length]=='\r' || b[length]=='\n')) {
			length--;
		}
		String msgS=new String(b, offset, length);
		if(msgS.trim().length() > 0 ) {
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
