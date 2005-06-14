// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 31812583-C312-4A6F-9A5E-036F8732D9C6

package net.spy.test;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import net.spy.util.SpyUtil;

/**
 * Test various things from net.spy.util.SpyUtil.
 */
public class SpyUtilTest extends TestCase {

    /**
     * Get an instance of SpyUtilTest.
     */
    public SpyUtilTest(String name) {
        super(name);
    }

	/** 
	 * Test the basics of split.
	 */
	public void testSplit() {
		String a[]=SpyUtil.split(",", "a,b,c");
		assertEquals(a.length, 3);
		assertEquals("a", a[0]);
		assertEquals("b", a[1]);
		assertEquals("c", a[2]);

		String a2[]=SpyUtil.split(",", "abc");
		assertEquals(a2.length, 1);
		assertEquals("abc", a2[0]);
	}

	/** 
	 * Test the basics of join.
	 */
	public void testJoin() {
		Collection a=new ArrayList();
		a.add("a");
		a.add("b");
		a.add("c");

		String col=SpyUtil.join(a, ",");
		String it=SpyUtil.join(a.iterator(), ",");
		String en=SpyUtil.join(new Vector(a).elements(), ",");

		assertEquals("a,b,c", col);
		assertEquals("a,b,c", it);
		assertEquals("a,b,c", en);
	}

	/** 
	 * Test the byte array to hex string thing.
	 */
	public void testByteAToHexString() {
		byte bytes[]={0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
		String s=SpyUtil.byteAToHexString(bytes);
		assertEquals("000102030405060708090a0b0c0d0e0f10", s);
	}

	/** 
	 * Test the boolean wrapper methods.
	 */
	public void testBooleanWrappers() {
		assertSame(Boolean.TRUE, SpyUtil.getBoolean("true"));
		assertSame(Boolean.FALSE, SpyUtil.getBoolean("false"));
		assertSame(Boolean.FALSE, SpyUtil.getBoolean("nope"));

		assertSame(Boolean.TRUE, SpyUtil.getBoolean(true));
		assertSame(Boolean.FALSE, SpyUtil.getBoolean(false));
	}

	/** 
	 * Test the reader to string conversion.
	 */
	public void testReaderAsString() throws Exception {
		String originalString="This is going to go the long way around.";
		ByteArrayInputStream bais=
			new ByteArrayInputStream(originalString.getBytes());
		Reader r=new InputStreamReader(bais);
		String readString=SpyUtil.getReaderAsString(r);
		r.close();
		bais.close();

		assertEquals(originalString, readString);
	}

	/** 
	 * Test the shuffle method.
	 */
	public void testShuffle() throws Exception {
		Object a[]=new Object[10];
		for(int i=0; i<10; i++) {
			a[i]=new Integer(i);
		}

		int match=0;
		Object b[]=SpyUtil.shuffle(a);
		for(int i=0; i<10; i++) {
			if(a[i].equals(b[i])) {
				match++;
			}
		}

		assertTrue("Result after shuffling is too similar.", match < 10);
	}

}
