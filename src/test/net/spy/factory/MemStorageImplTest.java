// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.factory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test the MemStorageImpl thing.
 */
public class MemStorageImplTest extends TestCase {

	private List<Ob> obs=null;
	private Storage<Ob> cache=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		CacheKeyFinder.setInstance(null);
		obs=Arrays.asList(new Ob(1, 10, "one"),
				new Ob(2, 10, "two"),
				new Ob(3, 11, "three"));
		cache=new MemStorageImpl<Ob>(obs);
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

	public void testCacheInsertion() throws Exception {
		assertNull(cache.getObject("id", 4));
		Ob o=new Ob(4, 44, "four");
		assertFalse(cache.getAllObjects().contains(o));
		cache.cacheInstance(o);
		assertSame(o, cache.getObject("id", 4));
		assertSame(o, cache.getObjects("ak", 44).iterator().next());
		assertTrue(cache.getAllObjects().contains(o));
	}

	public void testMissingAltKeyLookup() throws Exception {
		assertEquals(0, cache.getObjects("blah", 19).size());
		assertEquals(0, cache.getObjects("ak", 19).size());
	}
	
	public void testFailingCache() throws Exception {
		try {
			Storage<Fob> c=new MemStorageImpl<Fob>(
					Collections.singleton(new Fob()));
			fail("expected failure, got " + c);
		} catch(RuntimeException e) {
			// ok
		}
	}

	public void testSubclass() throws Exception {
		Storage<ObSub> c=new MemStorageImpl<ObSub>(
				Arrays.asList(
						new ObSub(1, 1, "one", "oneone"),
						new ObSub(2, 2, "two", "twotwo")));
		// Inherited
		assertEquals(1, c.getObject("id", 1).getAltKey());
		// This is a public method, so it'll get picked up
		assertEquals(1, c.getObjects("ak", 1).size());
		// This should be picked up from an interface
		assertEquals(1, c.getObjects("akak", 1).size());
		// This is in the subclass, so it will, too
		assertNotNull(c.getObject("other", "oneone"));
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

		@Override
		public String toString() {
			return "Ob#" + id + " ak=" + altKey + " n=" + name;
		}
	}

	public static interface AltAltKeyCache {
		@CacheKey(name="akak", type=CacheType.MULTI)
		public int getAltKey();
	}

	public static class ObSub extends Ob implements AltAltKeyCache {
		@CacheKey(name="other")
		public String other=null;
		public ObSub(int i, int a, String n, String o) {
			super(i, a, n);
			other=o;
		}
	}
}
