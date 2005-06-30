// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 31812583-C312-4A6F-9A5E-036F8732D9C6

package net.spy.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import junit.framework.TestCase;
import net.spy.util.Enumeriterator;
import net.spy.util.PwGen;
import net.spy.util.SPGen;
import net.spy.util.SpyUtil;

/**
 * Test various things from net.spy.util.
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

	/** 
	 * Test the Enumeriterator implementation.
	 */
	public void testEnumeriterator() throws Exception {
		Vector v=new Vector();
		v.add("a");
		v.add("b");
		v.add("c");
		for(Iterator i=new Enumeriterator(v.elements()); i.hasNext(); ) {
			i.next();
			try {
				i.remove();
			} catch(UnsupportedOperationException e) {
				assertEquals("No can do, chief.", e.getMessage());
				// pass
			}
		}
	}

	/** 
	 * Test the deHTMLer.
	 */
	public void testDeHTML() throws Exception {
		assertEquals("blah", SpyUtil.deHTML("<tag>blah</tag>"));
		assertEquals("blah", SpyUtil.deHTML("<tag thing=\"<a>\">blah</tag>"));
	}

	/** 
	 * Test the run class.
	 */
	public void testRunClass() throws Exception {
		String args[]=new String[0];
		SpyUtil.runClass("net.spy.test.SpyUtilTest", args);

		args=new String[1];
		args[0]="java.lang.Exception";
		try {
			SpyUtil.runClass("net.spy.test.SpyUtilTest", args);
			fail("Expected " + args[0]);
		} catch(Exception e) {
			// pass
		}

		// errors don't get thrown...perhaps they should
		args[0]="java.lang.Error";
		SpyUtil.runClass("net.spy.test.SpyUtilTest", args);
	}

	/** 
	 * Test the type name gen.
	 */
	public void testTypeNameGen() throws Exception {
		String tmpFile="/tmp/tn" + PwGen.getPass(12) + ".tmp";
		String args[]={tmpFile};
		SpyUtil.runClass("net.spy.util.TypeNameGen", args);
		File f=new File(tmpFile);
		f.delete();
	}

	/** 
	 * Test the interface implementor.
	 */
	public void testInterfaceImplementor() throws Exception {
		String tmpDir="/tmp/ii" + PwGen.getPass(12);
		String args[]={"-superclass", "java.util.HashMap",
			"-interface", "java.sql.ResultSet",
			"-outputclass", "test.TestResult",
			"-outputdir", tmpDir};
		SpyUtil.runClass("net.spy.util.InterfaceImplementor", args);

		SpyUtil.rmDashR(new File(tmpDir));
	}

	/** 
	 * Test the proxy interface implementor.
	 */
	public void testProxyInterfaceImplementor() throws Exception {
		String tmpDir="/tmp/pii" + PwGen.getPass(12);
		String args[]={"-superclass", "java.util.HashMap",
			"-interface", "java.sql.ResultSet",
			"-outputclass", "test.TestResult",
			"-outputdir", tmpDir};
		SpyUtil.runClass("net.spy.util.ProxyInterfaceImplementor", args);

		SpyUtil.rmDashR(new File(tmpDir));
	}

	private void generateSPT(String path, boolean override) throws Exception {
		String baseDir=System.getProperties().getProperty("basedir");
		assertNotNull(baseDir);
		File f=new File(baseDir + path);
		BufferedReader ireader=new BufferedReader(new FileReader(f));
		PrintWriter out=new PrintWriter(new OutputStreamWriter(
			new ByteArrayOutputStream()));

		SPGen spg=new SPGen("Tmp", ireader, out);
		spg.setVerbose(override);
		if(override) {
			spg.setSuperclass("net.spy.db.DBSQL");
			spg.setDbcpSuperclass("net.spy.db.DBCP");
			spg.setDbspSuperclass("net.spy.db.DBSP");
		}
		spg.generate();

		out.close();
		ireader.close();
	}

	private void generateSPT(String path) throws Exception {
		generateSPT(path, true);
		generateSPT(path, false);
	}

	/** 
	 * SPGen test.
	 */
	public void testSPGen() throws Exception {
		generateSPT("/src/test/net/spy/test/db/ThreeColumnTest.spt");
		generateSPT("/src/test/net/spy/test/db/CallTestFunc.spt");
		generateSPT("/src/test/net/spy/test/db/DialectTest.spt");
		generateSPT("/src/test/net/spy/test/db/CacheTest.txt");
		generateSPT("/src/test/net/spy/test/db/InterfaceTest.spt");
		generateSPT("/src/test/net/spy/test/db/ImplTest.spt");
		generateSPT("/src/test/net/spy/test/db/ImplTest2.txt");
		generateSPT("/src/test/net/spy/test/db/ThreeColumnOptional.txt");
		generateSPT("/src/test/net/spy/test/db/TestProc.txt");
		generateSPT("/src/test/net/spy/test/db/TestWithOutput.txt");

		try {
			generateSPT("/src/test/net/spy/test/db/Bad1.txt");
			fail("Expected an invalid section on Bad1");
		} catch(Exception e) {
			assertEquals("Unknown section: ``wtf''", e.getMessage());
		}

		try {
			generateSPT("/src/test/net/spy/test/db/Bad2.txt");
			fail("Expected an invalid type on Bad2");
		} catch(Exception e) {
			assertEquals("Invalid JDBC type: TYPE", e.getMessage());
		}

		try {
			generateSPT("/src/test/net/spy/test/db/Bad3.txt");
			fail("Expected invalid parameter on Bad3");
		} catch(Exception e) {
			assertEquals("No parameter for this default:  blah",
				e.getMessage());
		}
	}

	/** 
	 * Test for runclass.
	 */
	public static void main(String args[]) throws Throwable {
		if(args.length > 0) {
			Class c=Class.forName(args[0]);
			Throwable t=(Throwable)c.newInstance();
			throw t;
		}
	}

}
