// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: F48330CC-C858-46E1-A9DA-60AC1BB441FA

package net.spy.test;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import java.util.Map;
import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.Base64;
import net.spy.util.Base64OutputStream;
import net.spy.util.Base64InputStream;

/**
 * Base64 test.  Derived from the python base64 tests.
 */
public class Base64Test extends TestCase {

	private static final String CHARSET="UTF-8";

	private Map<String, String> cases=null;

	/**
	 * Get an instance of Base64Test.
	 */
	public Base64Test(String name) {
		super(name);
	}

	/** 
	 * Get the test suite.
	 */
	public static Test suite() {
		return new TestSuite(Base64Test.class);
	}

	protected void setUp() {
		cases=new HashMap();

		cases.put("d3d3LnB5dGhvbi5vcmc=", "www.python.org");
		cases.put("YQ==", "a");
		cases.put("YWI=", "ab");
		cases.put("YWJj", "abc");
		cases.put("", "");
		cases.put("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQ"
			+ "kNERUZHSElKS0xNTk9QUVJTVFVWV1hZWjAxMjM0\r\n"
			+ "NTY3ODkhQCMwXiYqKCk7Ojw+LC4gW117fQ==",
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "0123456789!@#0^&*();:<>,. []{}");
	}

	/** 
	 * Test the singleton semantics.
	 */
	public void testSingleton() {
		assertSame(Base64.getInstance(), Base64.getInstance());
	}

	private String encode(String s) throws Exception {
		Base64 b64=Base64.getInstance();
		String rv=b64.encode(s.getBytes(CHARSET));
		return(rv);
	}

	/** 
	 * Test base64 encoding of strings.
	 */
	public void testEncode() throws Exception {
		for(Map.Entry<String, String> me : cases.entrySet()) {
			assertEquals("Encode " + me.getValue(), me.getKey(),
				encode(me.getValue()));
		}
	}

	private String decode(String s) throws Exception {
		Base64 b64=Base64.getInstance();
		return(new String(b64.decode(s), CHARSET));
	}

	/** 
	 * Test base64 decodes.
	 */
	public void testDecode() throws Exception {
		for(Map.Entry<String, String> me : cases.entrySet()) {
			assertEquals("Decode " + me.getKey(), me.getValue(),
				decode(me.getKey()));
		}
	}

	/** 
	 * Test the isValidBase64Char implementation.
	 */
	public void testAlphabet() {
		Base64 b64=Base64.getInstance();
		char CHARMAP[]={
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
		};
		Map<Character, Boolean> allChars=new HashMap();
		for(int i=0; i<65535; i++) {
			allChars.put( (char)i, false);
		}
		for(int i=0; i<CHARMAP.length; i++) {
			allChars.put(CHARMAP[i], true);
		}
		for(Map.Entry<Character, Boolean> me : allChars.entrySet()) {
			assertEquals(me.getValue().booleanValue(),
				b64.isValidBase64Char(me.getKey()));
		}
	}

	/** 
	 * Test output stream implementation.
	 */
	public void testOutputStream() throws Exception {
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		for(Map.Entry<String, String> me : cases.entrySet()) {
			bos.reset();
			Base64OutputStream b64os=new Base64OutputStream(bos);
			b64os.write(me.getValue().getBytes(CHARSET));
			b64os.close();

			String result=new String(bos.toByteArray(), CHARSET);
			// The stream adds a newline to everything that doesn't end in a
			// newline.
			String expected=me.getKey() + "\r\n";
			if(me.getKey().length() % 76 == 0) {
				expected=me.getKey();
			}
			assertEquals("Stream Encode ``" + expected + "'' got ``"
				+ result + "''", expected, result);
		}
		bos.close();
	}

	/** 
	 * Test input stream implementation.
	 */
	public void testInputStream() throws Exception {
		byte buffer[]=new byte[1024];

		for(Map.Entry<String, String> me : cases.entrySet()) {
			byte input[]=me.getKey().getBytes(CHARSET);
			ByteArrayInputStream bis=new ByteArrayInputStream(input);

			Base64InputStream b64is=new Base64InputStream(bis);
			assertFalse(b64is.markSupported());

			int expectedAvailable=input.length * 3 / 4;
			assertEquals("available() failed for ``" + me.getKey() + "''",
				expectedAvailable, b64is.available());
			int bytesread=b64is.read(buffer, 0, buffer.length);
			b64is.close();

			String result=new String(buffer, 0, bytesread);
			String expected=me.getValue();
			assertEquals("Stream decode ``" + expected + "'' got ``"
				+ result + "''", expected, result);
		}
	}

}
