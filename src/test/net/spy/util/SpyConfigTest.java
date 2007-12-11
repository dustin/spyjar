// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.io.File;

import junit.framework.TestCase;

/**
 * Test SpyConfig stuff.
 */
public class SpyConfigTest extends TestCase {

	private File confFile=null;
	private File nonsenseFile=null;

	/**
	 * Get an instance of SpyConfigTest.
	 */
	public SpyConfigTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() {
		nonsenseFile=new File("/some/nonsense/file");
		confFile=new File(System.getProperties().get("basedir")
			+ "/etc/test.conf");
	}

	/**
	 * Test reading a non-existent file.
	 */
	public void testReadingNonExistent() throws Exception {
		SpyConfig conf=new SpyConfig(nonsenseFile);
		assertEquals(0, conf.size());
		conf=new SpyConfig(nonsenseFile);
		assertEquals(0, conf.size());

		assertFalse(conf.loadConfig(nonsenseFile));
		File[] a=new File[1];
		a[0]=nonsenseFile;
		assertFalse(conf.loadConfig(a));
	}

	/**
	 * Test loading a real config.
	 */
	public void testLoadingConfig() throws Exception {
		SpyConfig conf1=new SpyConfig(confFile);
		assertEquals(5, conf1.size());

		SpyConfig conf2=new SpyConfig();
		assertEquals(0, conf2.size());
		assertTrue(conf2.loadConfig(confFile));
		assertEquals(5, conf2.size());

		File[] a=new File[2];
		a[0]=new File("/some/nonsense/file");
		a[1]=confFile;
		SpyConfig conf3=new SpyConfig();
		assertEquals(0, conf3.size());
		assertTrue(conf3.loadConfig(a));
		assertEquals(5, conf3.size());
	}

	/**
	 * Test the orput stuff.
	 */
	public void testOrPut() throws Exception {
		SpyConfig conf=new SpyConfig(confFile);
		assertEquals("username", conf.get("dbUser"));
		conf.orput("dbUser", "blah");
		assertEquals("username", conf.get("dbUser"));
		conf.put("dbUser", "blah");
		assertEquals("blah", conf.get("dbUser"));
		assertNull(conf.get("testEntry"));
		conf.orput("testEntry", "yep");
		assertEquals("yep", conf.get("testEntry"));
	}

	/**
	 * Test conf getting with defaults.
	 */
	public void testDefs() throws Exception {
		SpyConfig conf=new SpyConfig();
		assertNull(conf.get("blah"));
		assertNull(conf.get("blah", null));
		assertEquals("blah", conf.get("blah", "blah"));
		conf.put("blah", "woo");
		assertEquals("woo", conf.get("blah", "blah"));
		assertEquals("woo", conf.get("blah", null));

		assertEquals(13, conf.getInt("number", 13));
		conf.put("number", "17");
		assertEquals(17, conf.getInt("number", 13));
	}

}
