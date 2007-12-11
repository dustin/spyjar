// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test the ring buffer functionality.
 */
public class NetStringTest extends TestCase {

	/**
	 * Get an instance of NetStringTest.
	 */
	public NetStringTest(String name) {
		super(name);
	}

	/**
	 * Get the test suite.
	 */
	public static Test suite() {
		return new TestSuite(NetStringTest.class);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Test the encoder.
	 */
	public void testEncoder() throws IOException {
		String encoding="UTF-8";
		NetStringEncoder nse=new NetStringEncoder(encoding);
		ByteArrayOutputStream os=new ByteArrayOutputStream(64);
		nse.encodeString("test", os);
		String encoded=os.toString(encoding);
		assertEquals("Encoded string not correct", encoded, "4:test,");

		os.reset();
		nse.encodeString("", os);
		encoded=os.toString(encoding);
		assertEquals("Encoded string not correct", encoded, "0:,");

		os.reset();
		nse.encodeString("a", os);
		nse.encodeString("bc", os);
		nse.encodeString("def", os);
		encoded=os.toString(encoding);
		assertEquals("Encoded string not correct", encoded,
			"1:a,2:bc,3:def,");

		try {
			nse.encodeString(null, os);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}
	}

	/**
	 * Test the decoder.
	 */
	public void testDecoder() throws IOException {
		String encoding="UTF-8";
		NetStringEncoder nse=new NetStringEncoder(encoding);
		NetStringDecoder nsd=new NetStringDecoder(encoding);
		ByteArrayOutputStream os=new ByteArrayOutputStream(256);

		String[] tmp={"", "a", "bc", "def", "ghij", "this is a mofo test"};

		for(int i=0; i<tmp.length; i++) {
			nse.encodeString(tmp[i], os);
		}

		String encoded=os.toString(encoding);
		// System.out.println("Encoded " + encoded);
		ByteArrayInputStream is=new ByteArrayInputStream(
			encoded.getBytes(encoding));

		for(int i=0; i<tmp.length; i++) {
			String read=nsd.decodeString(is);
			// System.out.println("Read " + read);
			assertEquals("Encode and decode do not match", tmp[i], read);
		}
	}

	// Verify exceptions are thrown for bad data
	private void negativeTest(String s)
		throws java.io.UnsupportedEncodingException {

		String encoding="UTF-8";
		NetStringDecoder nsd=new NetStringDecoder(encoding);
		ByteArrayInputStream is=new ByteArrayInputStream(
			s.getBytes(encoding));
		try {
			String tmp=nsd.decodeString(is);
			fail("Expected failure for ``" + s + "'', got ``" + tmp + "''");
		} catch(IOException e) {
			// pass
		}
	}

	/**
	 * Test the decoder with bad input.
	 */
	public void testBrokenDecoder() throws IOException {
		negativeTest("2:a,");
		negativeTest("0:a,");
		negativeTest("2a:a,");
		negativeTest("135235226232:a,");
		negativeTest("70000:a,");
		negativeTest("700");
	}

}
