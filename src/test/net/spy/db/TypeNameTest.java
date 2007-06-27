// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.lang.reflect.Field;
import java.sql.Types;

import junit.framework.TestCase;

/**
 * Test the type names implementation.
 */
public class TypeNameTest extends TestCase {

	/** 
	 * Test all type names.
	 */
	public void testTypeNames() throws Exception {
		Field[] fields=Types.class.getDeclaredFields();
		for(int i=0; i<fields.length; i++) {
			int obId=((Integer)fields[i].get(null)).intValue();
			String myTypeName=TypeNames.getTypeName(obId);

			assertEquals(fields[i].getName(), myTypeName);
		}
		assertEquals("Unknown#" + Integer.MAX_VALUE,
			TypeNames.getTypeName(Integer.MAX_VALUE));
	}

}
