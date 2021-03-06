// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A FilterOutputStream that encodes data into Base64.
 */
public class Base64OutputStream extends FilterOutputStream {

	private final Base64 base64;
	private int currentByte=0;
	private int currentOutput=0;
	private final byte[] buffer;
	private final byte[] crlf;

	/**
	 * Get a new Base64OutputStream encoding the given OutputStream.
	 */
	public Base64OutputStream(OutputStream os) {
		super(os);
		base64=new Base64();
		buffer=new byte[3];
		crlf=new byte[2];
		crlf[0]=(byte)'\r';
		crlf[1]=(byte)'\n';
	}

	/**
	 * Writes len bytes from the specified byte array starting at offset off
	 * to this output stream.  See the documentation for FilterOutputStream
	 * for more details.
	 *
	 * @see FilterOutputStream
	 */
	@Override
	public void write(byte data[], int offset, int length) throws IOException {
		for(int i=offset; i<offset+length; i++) {
			write(data[i]);
		}
	}

	/**
	 * Close this stream and finish up the Base64.
	 */
	@Override
	public void close() throws IOException {
		if(currentByte>0) {
			byte[] tmp=new byte[currentByte];
			System.arraycopy(buffer, 0, tmp, 0, currentByte);
			out.write(base64.encode(tmp).getBytes());
			out.write(crlf);
			currentByte=0;
			currentOutput=0;
		} else {
			// Unless this is a new line, add a newline.
			if(currentOutput!=0) {
				out.write(crlf);
			}
		}
		super.close();
	}

	/**
	 * Write the given byte to the underlying OutputStream.
	 */
	public void write(byte datum) throws IOException {
		buffer[currentByte++]=datum;
		if(currentByte==3) {
			out.write(base64.encode(buffer).getBytes());
			currentByte=0;
			currentOutput+=4;
		}
		if(currentOutput==76) {
			currentOutput=0;
			out.write(crlf);
		}
	}

}
