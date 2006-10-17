// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 29A4CD57-0CCC-4614-9EF4-E4DA0478CD5C

package net.spy.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import net.spy.factory.CacheKey;
import net.spy.factory.CacheType;
import net.spy.factory.Storage;

/**
 * Test the HashCacheEntry thing.
 */
public class FactoryStorageTest extends TestCase {

	private List<Ob> obs=null;
	private Storage<Ob> cache=null;

	protected void setUp() throws Exception {
		super.setUp();
		obs=Arrays.asList(new Ob(1, 10, "one"),
				new Ob(2, 10, "two"),
				new Ob(3, 11, "three"));
		cache=new Storage<Ob>(obs);
	}

	public void testGetAll() throws Exception {
		assertEquals(new HashSet<Ob>(obs),
				new HashSet<Ob>(cache.getAllObjects()));
		assertEquals(3, new HashSet<Ob>(obs).size());
	}

	public void testIdLookup() throws Exception {
		assertSame(obs.get(0), cache.getObject("id", 1));
		assertSame(obs.get(1), cache.getObject("id", 2));
		assertSame(obs.get(2), cache.getObject("id", 3));
		assertNull(cache.getObject("id", 4));
	}

	public void testAltKeyLookup() throws Exception {
		assertEquals(2, cache.getObjects("ak", 10).size());
	}

	public void testMissingAltKeyLookup() throws Exception {
		assertEquals(0, cache.getObjects("blah", 19).size());
		assertEquals(0, cache.getObjects("ak", 19).size());
	}
	
	public void testFailingCache() throws Exception {
		try {
			Storage<Fob> c=new Storage<Fob>(
					Collections.singleton(new Fob()));
			fail("expected failure, got " + c);
		} catch(RuntimeException e) {
			// ok
		}
	}

	public static class Fob {
		@CacheKey(name="x")
		public int getSome() throws Exception {
			throw new Exception("Arr");
		}
	}

	public static class Ob {
		@CacheKey(name="id")
		private int id=0;
		private int altKey=0;
		private String name=null;

		public Ob(int i, int a, String n) {
			super();
			id=i;
			altKey=a;
			name=n;
		}

		@CacheKey(name="ak", type=CacheType.MULTI)
		public int getAltKey() {
			return altKey;
		}

		public String toString() {
			return "Ob#" + id + " ak=" + altKey + " n=" + name + "}";
		}
	}
}
