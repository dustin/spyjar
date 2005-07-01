// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: F4EAE49F-4B3A-4526-8B1E-C8B718082F7C

package net.spy.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.spy.util.SpyToker;
import net.spy.util.SpyUtil;

/**
 * Tokenizer test cases.
 */
public class TokerTest extends TestCase {

	private Map vars=null;
	private SpyToker toker=null;

    /**
     * Get an instance of TokerTest.
     */
    public TokerTest(String name) {
        super(name);
    }

	protected void setUp() {
		toker=new SpyToker();
		vars=new HashMap();
		vars.put("t1", "test1");
		vars.put("t2", "test2");
	}

	/** 
	 * Test basic tokenization.
	 */
	public void testBasicTokenizer() {
		assertEquals("/tmp/blah", toker.tokenizeString("/tmp/blah", vars));
		assertEquals("/tmp/test1", toker.tokenizeString("/tmp/%t1%", vars));
		assertEquals("ttest11", toker.tokenizeString("t%t1%1", vars));
		assertEquals("%%t1", toker.tokenizeString("%%t1", vars));
		assertEquals("%blah%", toker.tokenizeString("%blah%", vars));
		assertEquals("%t2", toker.tokenizeString("%t2", vars));
		assertEquals("test1test2", toker.tokenizeString("%t1%%t2%", vars));
		assertEquals("%", toker.tokenizeString("%", vars));
	}

	/** 
	 * Test tokenizing files.
	 */
	public void testFileTokenizer() throws Exception {
		assertNull(toker.tokenize(new File("/some/nonsense/path"), vars));

		String path=System.getProperties().getProperty("basedir")
			+ "/etc/test.conf";
		File theFile=new File(path);
		assertEquals(SpyUtil.getFileData(theFile),
			toker.tokenize(theFile, vars));
	}

}