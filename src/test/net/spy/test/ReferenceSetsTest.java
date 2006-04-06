// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 2AA64DDF-1110-11D9-B77E-000A957659CC

package net.spy.test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import junit.framework.TestCase;
import net.spy.util.SoftHashSet;
import net.spy.util.WeakHashSet;

/**
 * Test the reference hash set implementations.
 */
public class ReferenceSetsTest extends TestCase {

	/**
	 * Get an instance of ReferenceSetsTest.
	 */
	public ReferenceSetsTest(String name) {
		super(name);
	}

	/** 
	 * Test the basic functionality of the weak hash set.
	 * -- this test is disabled, as it doesn't seem to work reliably on my
	 * -- core duo.  I wouldn't necessarily expect it to.
	 */
	public void xtestGCWeakHash() {
		Set<String> s=new java.util.HashSet();
		for(int i=0; i<100; i++) {
			s.add("Blah" + i);
		}

		WeakHashSet<String> whs=new WeakHashSet(s);

		assertEquals("Size didn't match", whs.size(), s.size());

		// Remove some items.
		int i=0;
		for(Iterator it=s.iterator(); it.hasNext();) {
			it.next();
			if( (i++%2) == 0) {
				it.remove();
			}
		}

		// Do a garbage collection.
		System.gc();

		// Recompare the sizes.
		assertEquals("Size didn't match after removals", whs.size(), s.size());
	}

	private void equalityTestFor(Set<String> rhs) {
		Set<String> s=new java.util.HashSet();
		for(int i=0; i<100; i++) {
			s.add("Blah" + i);
		}

		assertTrue(rhs.isEmpty());
		rhs.addAll(s);
		// Add it again, since it should end up being the same
		rhs.addAll(s);
		assertFalse(rhs.isEmpty());

		// Recompare the sizes.
		assertEquals("Size didn't match after adds", s.size(), rhs.size());

		for(String ob : s) {
			assertTrue(rhs.contains(ob));
		}

		for(String ob : rhs) {
			assertTrue(s.contains(ob));
		}
	}

	private void basicTestFor(Set s) {
		assertFalse(s.contains("a"));
		assertFalse(s.contains(null));
		s.add("a");
		assertTrue(s.contains("a"));
		// I'm not testing null adds yet because it's not supported by
		// SoftHashSet.
	}

	public void testBasicSetStuff() {
		basicTestFor(new WeakHashSet());
		basicTestFor(new SoftHashSet());
	}

	/** 
	 * Verify equality (lookup, etc...) works.
	 */
	public void testEquality() {
		equalityTestFor(new WeakHashSet(100));
		equalityTestFor(new SoftHashSet(100));
	}

	private void removalTestFor(Set rhs) {
		Set<String> s=new java.util.HashSet();
		for(int i=0; i<100; i++) {
			s.add("Blah" + i);
		}

		assertTrue(rhs.isEmpty());
		rhs.addAll(s);
		assertFalse(rhs.isEmpty());

		assertFalse(rhs.remove("TestObject"));
		for(String ob : s) {
			assertTrue(rhs.remove(ob));
		}

		assertTrue(rhs.isEmpty());
		rhs.addAll(s);
		assertFalse(rhs.isEmpty());

		rhs.clear();
		assertTrue(rhs.isEmpty());
	}

	/** 
	 * Test refset removal.
	 */
	public void testRemoval() {
		removalTestFor(new WeakHashSet(100));
		try {
			removalTestFor(new SoftHashSet(100));
			fail("I didn't know SoftHashSet supported iterator removal!?");
		} catch(UnsupportedOperationException e) {
			// pass
		}
	}

	private void runIteratorTest(Set s) {
		Iterator i=null;
		for(i=s.iterator(); i.hasNext();) {
			i.next();
		}
		try {
			i.next();
			fail("Iterator let me have more stuff than it had.");
		} catch(NoSuchElementException e) {
			// pass
		}
	}

	/** 
	 * Test the iterators.
	 */
	public void testIterators() {
		Set<String> s=new java.util.HashSet();
		for(int i=0; i<10; i++) {
			s.add("Blah" + i);
		}

		runIteratorTest(new SoftHashSet(s));
		runIteratorTest(new WeakHashSet(s));
	}

	/** 
	 * Constructor tests.
	 */
	public void testConstructors() {
		Set<String> s=new java.util.HashSet();
		for(int i=0; i<10; i++) {
			s.add("Blah" + i);
		}

		SoftHashSet shs=new SoftHashSet(s);
		WeakHashSet whs=new WeakHashSet(s);

		assertTrue(shs.containsAll(s));
		assertTrue(s.containsAll(shs));
		assertTrue(whs.containsAll(s));
		assertTrue(s.containsAll(whs));

		shs=new SoftHashSet();
		shs.addAll(s);
		whs=new WeakHashSet();
		whs.addAll(s);

		assertTrue(shs.containsAll(s));
		assertTrue(s.containsAll(shs));
		assertTrue(whs.containsAll(s));
		assertTrue(s.containsAll(whs));

		try {
			SoftHashSet nshs=new SoftHashSet(null);
			fail("Made a SoftHashSet from null:  " + nshs);
		} catch(NullPointerException e) {
			assertEquals("Null collection provided to ReferenceSet",
				e.getMessage());
		}

		try {
			WeakHashSet nwhs=new WeakHashSet(null);
			fail("Made a WeakHashSet from null:  " + nwhs);
		} catch(NullPointerException e) {
			assertEquals("Null collection provided to WeakHashSet",
				e.getMessage());
		}
	}

}
