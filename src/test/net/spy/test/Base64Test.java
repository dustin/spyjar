// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: F48330CC-C858-46E1-A9DA-60AC1BB441FA

package net.spy.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.spy.util.Base64;
import net.spy.util.Base64InputStream;
import net.spy.util.Base64OutputStream;

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
		cases=new HashMap<String, String>();

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
		for(Iterator i=cases.entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			assertEquals("Encode " + me.getValue(), me.getKey(),
				encode((String)me.getValue()));
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
		for(Iterator i=cases.entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			assertEquals("Decode " + me.getKey(), me.getValue(),
				decode((String)me.getKey()));
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
		Map<Character, Boolean> allChars=new HashMap<Character, Boolean>();
		for(int i=0; i<65535; i++) {
			allChars.put( new Character((char)i), Boolean.FALSE);
		}
		for(int i=0; i<CHARMAP.length; i++) {
			allChars.put(new Character(CHARMAP[i]), Boolean.TRUE);
		}
		for(Iterator i=allChars.entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			assertEquals(((Boolean)me.getValue()).booleanValue(),
				b64.isValidBase64Char(((Character)me.getKey()).charValue()));
		}
	}

	/** 
	 * Test output stream implementation.
	 */
	public void testOutputStream() throws Exception {
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		for(Iterator i=cases.entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();

			bos.reset();
			Base64OutputStream b64os=new Base64OutputStream(bos);
			b64os.write(((String)me.getValue()).getBytes(CHARSET));
			b64os.close();

			String result=new String(bos.toByteArray(), CHARSET);
			// The stream adds a newline to everything that doesn't end in a
			// newline.
			String expected=me.getKey() + "\r\n";
			if(((String)me.getKey()).length() % 76 == 0) {
				expected=(String)me.getKey();
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

		for(Iterator i=cases.entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();

			byte input[]=((String)me.getKey()).getBytes(CHARSET);
			ByteArrayInputStream bis=new ByteArrayInputStream(input);

			Base64InputStream b64is=new Base64InputStream(bis);
			assertFalse(b64is.markSupported());

			int expectedAvailable=input.length * 3 / 4;
			assertEquals("available() failed for ``" + me.getKey() + "''",
				expectedAvailable, b64is.available());
			int bytesread=b64is.read(buffer, 0, buffer.length);
			b64is.close();

			String result=new String(buffer, 0, bytesread);
			String expected=(String)me.getValue();
			assertEquals("Stream decode ``" + expected + "'' got ``"
				+ result + "''", expected, result);
		}
	}

	/** 
	 * Test that streaming base64 yields the same result as whole chunk
	 * encoding.
	 */
	public void testBlockOutput() throws Exception {
		HashMap<String, Comparable> hm=new HashMap<String, Comparable>();
		hm.put("Some Key", new Integer(13));
		hm.put("Some Other Key", "Another value");

		ByteArrayOutputStream bos1=new ByteArrayOutputStream();
		Base64OutputStream b64os=new Base64OutputStream(bos1);
		ObjectOutputStream oos1=new ObjectOutputStream(b64os);

		oos1.writeObject(hm);
		oos1.close();
		b64os.close();
		bos1.close();
		String encoded1=new String(bos1.toByteArray());

		ByteArrayOutputStream bos2=new ByteArrayOutputStream();
		ObjectOutputStream oos2=new ObjectOutputStream(bos2);
		oos2.writeObject(hm);
		oos2.close();
		bos2.close();

		String encoded2=Base64.getInstance().encode(bos2.toByteArray());

		assertEquals(encoded2.trim(), encoded1.trim());
	}

	/** 
	 * Test that streaming base64 in yields the same result as whole chunk
	 * decoding.
	 */
	public void testBlockInput() throws Exception {
		HashMap<String, Comparable> hm=new HashMap<String, Comparable>();
		hm.put("Some Key", new Integer(13));
		hm.put("Some Other Key", "Another value");

		// This will produce our seed data.
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		ObjectOutputStream oos=new ObjectOutputStream(bos);
		oos.writeObject(hm);

		String src=Base64.getInstance().encode(bos.toByteArray());

		byte b1[]=Base64.getInstance().decode(src);

		ByteArrayInputStream bis=new ByteArrayInputStream(
			src.getBytes(CHARSET));
		Base64InputStream b64is=new Base64InputStream(bis);
		byte b2[]=new byte[b1.length];
		int bytesRead=b64is.read(b2);

		assertEquals(b1.length, bytesRead);
		for(int i=0; i<bytesRead; i++) {
			assertEquals("Differs at " + i, b1[i], b2[i]);
		}
	}

	/** 
	 * Test object serialization and compression and all that into base64.
	 * This particular case failed in the field.
	 */
	public void testSerializingStream() throws Exception {
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		Base64OutputStream b64os=new Base64OutputStream(bos);
		GZIPOutputStream gzos=new GZIPOutputStream(b64os);
		ObjectOutputStream oos=new ObjectOutputStream(gzos);

		HashMap<String, Comparable> hm=new HashMap<String, Comparable>();
		hm.put("Some Key", new Integer(13));
		hm.put("Some Other Key", "Another value");

		oos.writeObject(hm);

		oos.close();
		gzos.close();
		b64os.close();
		bos.close();

		String encoded=new String(bos.toByteArray());

		// Now try to pull them back
		ByteArrayInputStream bis=new ByteArrayInputStream(
			encoded.getBytes(CHARSET));
		Base64InputStream b64is=new Base64InputStream(bis);
		GZIPInputStream gzis=new GZIPInputStream(b64is);
		ObjectInputStream ois=new ObjectInputStream(gzis);

		HashMap readMap=(HashMap)ois.readObject();

		ois.close();
		gzis.close();
		b64is.close();
		bis.close();

		assertEquals(hm, readMap);
	}

}
