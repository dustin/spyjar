// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPathTest.java,v 1.4 2002/11/07 18:22:45 dustin Exp $

package net.spy.test;

import java.lang.ref.WeakReference;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.SPNode;
import net.spy.util.AbstractSPNode;
import net.spy.util.SPVertex;
import net.spy.util.ShortestPathFinder;
import net.spy.util.ShortestPath;
import net.spy.util.NoPathException;

/**
 * Test a weak hash set.
 */
public class ShortestPathTest extends TestCase {

	private Map nodes=null;
	private StringNode a=null;
	private StringNode b=null;
	private StringNode c=null;
	private StringNode d=null;
	private StringNode e=null;
	private StringNode f=null;
	private StringNode g=null;

	/**
	 * Get an instance of ShortestPathTest.
	 */
	public ShortestPathTest(String name) {
		super(name);
	}

	/** 
	 * Get the suite.
	 */
	public static Test suite() {
		return new TestSuite(ShortestPathTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Create the collection of nodes.
	 */
	protected void setUp() {
		nodes=new java.util.TreeMap();

		a=new StringNode("A");
		b=new StringNode("B");
		c=new StringNode("C");
		d=new StringNode("D");
		e=new StringNode("E");
		f=new StringNode("F");
		g=new StringNode("G");

		// Add the nodes to the collection
		nodes.put("A", a);
		nodes.put("B", b);
		nodes.put("C", c);
		nodes.put("D", d);
		nodes.put("E", e);
		nodes.put("F", f);
		nodes.put("G", g);

		// A -> B    A -> C (cost 15)
		a.linkTo(b);
		a.linkTo(c, 15);

		// B -> C
		b.linkTo(c);

		// C -> D   C -> E  C -> F
		c.linkTo(d);
		c.linkTo(e);
		c.linkTo(f);

		// D -> E
		// d.linkTo(e);
		// D -> C at a higher cost, giving it a path to E
		d.linkTo(c, 100);

		// Link e to itself
		e.linkTo(e, 10);

		// And f links to g
		f.linkTo(g, 10);

		// calculate the paths
		ShortestPathFinder spf=new ShortestPathFinder();
		spf.calculatePaths(nodes.values());
	}

	// verify a link matches the way we want
	private void assertLinkMatch(SPNode a, SPNode b, SPNode expectedNextHop,
		int cost) {

		SPVertex nextHop=a.getNextHop(b);
		if(expectedNextHop == null) {
			assertNull("Expected no link from " + a + " to " + b, nextHop);
		} else {
			assertNotNull("Expected a link from " + a + " to " + b, nextHop);
			assertSame("Expected link from " + a + " to " + b
				+ " to be via " + expectedNextHop
				+ ", got " + nextHop.getTo(), nextHop.getTo(), expectedNextHop);
			assertEquals("Incorrect cost for " + a + " -> " + b,
				cost, nextHop.getCost());
		}
	}

	/** 
	 * Test a basic SP find.
	 */
	public void testSPFind() {
		// Print out links...this is kinda big and ugly
		/*
		for(Iterator i=nodes.values().iterator(); i.hasNext();) {
			StringNode sn=(StringNode)i.next();
			sn.dump();
		}
		*/

		// These are manual tests.  Based on the way I configured the graph
		// above, these are all of the expected values (least costly
		// next-hops).

		// A -> A -- no match
		assertLinkMatch(a, a, null, 0);
		// A -> B == 10 via B
		assertLinkMatch(a, b, b, 10);
		// A -> C == 15 via C
		assertLinkMatch(a, c, c, 15);
		// A -> D == 25 via C
		assertLinkMatch(a, d, c, 25);
		// A -> E == 25 via C
		assertLinkMatch(a, e, c, 25);
		// A -> F == 25 via C
		assertLinkMatch(a, f, c, 25);
		// A -> G == 35 via C
		assertLinkMatch(a, g, c, 35);

		// B -> A -- doesn't exist
		assertLinkMatch(b, a, null, 0);
		// B -> B -- doesn't exist
		assertLinkMatch(b, b, null, 0);
		// B -> C == 10 via C
		assertLinkMatch(b, c, c, 10);
		// B -> D == 20 via C
		assertLinkMatch(b, d, c, 20);
		// B -> E == 20 via C
		assertLinkMatch(b, e, c, 20);
		// B -> F == 20 via C
		assertLinkMatch(b, f, c, 20);
		// B -> G == 30 via C
		assertLinkMatch(b, g, c, 30);

		// C -> A won't go
		assertLinkMatch(c, a, null, 0);
		// C -> B won't go
		assertLinkMatch(c, b, null, 0);
		// C -> C via D?
		assertLinkMatch(c, c, d, 110);
		// C -> D == 10 via D
		assertLinkMatch(c, d, d, 10);
		// C -> E == 10 via E
		assertLinkMatch(c, e, e, 10);
		// C -> F == 10 via F
		assertLinkMatch(c, f, f, 10);
		// C -> G == 20 via F
		assertLinkMatch(c, g, f, 20);

		// D -> A won't go
		assertLinkMatch(d, a, null, 0);
		// D -> B won't go
		assertLinkMatch(d, b, null, 0);
		// D -> C via C
		assertLinkMatch(d, c, c, 100);
		// D -> D via C
		assertLinkMatch(d, d, c, 110);
		// D -> E via C
		assertLinkMatch(d, e, c, 110);
		// D -> F via C
		assertLinkMatch(d, f, c, 110);
		// D -> G via C
		assertLinkMatch(d, g, c, 120);

		// E Goes nowhere except E
		assertLinkMatch(e, a, null, 0);
		assertLinkMatch(e, b, null, 0);
		assertLinkMatch(e, c, null, 0);
		assertLinkMatch(e, d, null, 0);
		assertLinkMatch(e, e, e, 10);
		assertLinkMatch(e, f, null, 0);
		assertLinkMatch(e, g, null, 0);
	}

	/** 
	 * Do a couple of quick ShortestPath tests.
	 */
	public void testShortestPath() throws NoPathException {
		ShortestPath sp=new ShortestPath(a, b);
		assertEquals("ShortestPath from A -> B", 1, sp.size());
		try {
			sp=new ShortestPath(a, a);
			fail("Expected to not find a path from A -> A, found " + sp);
		} catch(NoPathException e) {
			// Success
		}
		sp=new ShortestPath(a, c);
		assertEquals("ShortestPath from A -> C:  " + sp, 1, sp.size());
		sp=new ShortestPath(a, d);
		assertEquals("ShortestPath from A -> D:  " + sp, 2, sp.size());
		sp=new ShortestPath(a, e);
		assertEquals("ShortestPath from A -> E:  " + sp, 2, sp.size());

		sp=new ShortestPath(d, e);
		assertEquals("ShortestPath from D -> E:  " + sp, 2, sp.size());

		sp=new ShortestPath(e, e);
		assertEquals("ShortestPath from E -> E:  " + sp, 1, sp.size());
	}

	/** 
	 * Verify garbage collection can clean up the instances.
	 */
	public void testCleanup() {
		WeakReference aref=new WeakReference(a);
		WeakReference bref=new WeakReference(b);
		WeakReference cref=new WeakReference(c);
		WeakReference dref=new WeakReference(d);
		WeakReference eref=new WeakReference(e);

		// Verify the reference is alive
		assertNotNull("Reference to A is broken", aref.get());
		assertNotNull("Reference to B is broken", bref.get());
		assertNotNull("Reference to C is broken", cref.get());
		assertNotNull("Reference to D is broken", dref.get());
		assertNotNull("Reference to E is broken", eref.get());

		// Kill them all.
		nodes=null;
		a=null;
		b=null;
		c=null;
		d=null;
		e=null;

		// Pick up the trash
		System.gc();

		// Verify the reference is dead
		assertNull("Node A is still around", aref.get());
		assertNull("Node B is still around", bref.get());
		assertNull("Node C is still around", cref.get());
		assertNull("Node D is still around", dref.get());
		assertNull("Node E is still around", eref.get());
	}

	/** 
	 * Validate a large graph with a lot of links may be created.
	 */
	public void testBigGraph() {
		List tmpList=new ArrayList(100);

		tmpList.add(new StringNode("0"));
		tmpList.add(new StringNode("1"));
		tmpList.add(new StringNode("2"));
		tmpList.add(new StringNode("3"));
		tmpList.add(new StringNode("4"));
		tmpList.add(new StringNode("5"));
		tmpList.add(new StringNode("6"));
		tmpList.add(new StringNode("7"));
		tmpList.add(new StringNode("8"));
		tmpList.add(new StringNode("9"));
		tmpList.add(new StringNode("10"));
		tmpList.add(new StringNode("11"));
		tmpList.add(new StringNode("12"));
		tmpList.add(new StringNode("13"));
		tmpList.add(new StringNode("14"));
		tmpList.add(new StringNode("15"));
		tmpList.add(new StringNode("16"));
		tmpList.add(new StringNode("17"));
		tmpList.add(new StringNode("18"));
		tmpList.add(new StringNode("19"));
		tmpList.add(new StringNode("20"));
		tmpList.add(new StringNode("21"));
		tmpList.add(new StringNode("22"));
		tmpList.add(new StringNode("23"));
		tmpList.add(new StringNode("24"));
		tmpList.add(new StringNode("25"));
		tmpList.add(new StringNode("26"));
		tmpList.add(new StringNode("27"));
		tmpList.add(new StringNode("28"));
		tmpList.add(new StringNode("29"));
		tmpList.add(new StringNode("30"));
		tmpList.add(new StringNode("31"));
		tmpList.add(new StringNode("32"));
		tmpList.add(new StringNode("33"));
		tmpList.add(new StringNode("34"));
		tmpList.add(new StringNode("35"));
		tmpList.add(new StringNode("36"));
		tmpList.add(new StringNode("37"));
		tmpList.add(new StringNode("38"));
		tmpList.add(new StringNode("39"));
		tmpList.add(new StringNode("40"));
		tmpList.add(new StringNode("41"));
		tmpList.add(new StringNode("42"));
		tmpList.add(new StringNode("43"));
		tmpList.add(new StringNode("44"));
		tmpList.add(new StringNode("45"));
		tmpList.add(new StringNode("46"));
		tmpList.add(new StringNode("47"));
		tmpList.add(new StringNode("48"));
		tmpList.add(new StringNode("49"));
		tmpList.add(new StringNode("50"));
		tmpList.add(new StringNode("51"));
		tmpList.add(new StringNode("52"));
		tmpList.add(new StringNode("53"));
		tmpList.add(new StringNode("54"));
		tmpList.add(new StringNode("55"));
		tmpList.add(new StringNode("56"));
		tmpList.add(new StringNode("57"));
		tmpList.add(new StringNode("58"));
		tmpList.add(new StringNode("59"));
		tmpList.add(new StringNode("60"));
		tmpList.add(new StringNode("61"));
		tmpList.add(new StringNode("62"));
		tmpList.add(new StringNode("63"));
		tmpList.add(new StringNode("64"));
		tmpList.add(new StringNode("65"));
		tmpList.add(new StringNode("66"));
		tmpList.add(new StringNode("67"));
		tmpList.add(new StringNode("68"));
		tmpList.add(new StringNode("69"));
		tmpList.add(new StringNode("70"));
		tmpList.add(new StringNode("71"));
		tmpList.add(new StringNode("72"));
		tmpList.add(new StringNode("73"));
		tmpList.add(new StringNode("74"));
		tmpList.add(new StringNode("75"));
		tmpList.add(new StringNode("76"));
		tmpList.add(new StringNode("77"));
		tmpList.add(new StringNode("78"));
		tmpList.add(new StringNode("79"));
		tmpList.add(new StringNode("80"));
		tmpList.add(new StringNode("81"));
		tmpList.add(new StringNode("82"));
		tmpList.add(new StringNode("83"));
		tmpList.add(new StringNode("84"));
		tmpList.add(new StringNode("85"));
		tmpList.add(new StringNode("86"));
		tmpList.add(new StringNode("87"));
		tmpList.add(new StringNode("88"));
		tmpList.add(new StringNode("89"));
		tmpList.add(new StringNode("90"));
		tmpList.add(new StringNode("91"));
		tmpList.add(new StringNode("92"));
		tmpList.add(new StringNode("93"));
		tmpList.add(new StringNode("94"));
		tmpList.add(new StringNode("95"));
		tmpList.add(new StringNode("96"));
		tmpList.add(new StringNode("97"));
		tmpList.add(new StringNode("98"));
		tmpList.add(new StringNode("99"));

		StringNode tmpNode=null;
		tmpNode=(StringNode)tmpList.get(62);
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode=(StringNode)tmpList.get(40);
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode=(StringNode)tmpList.get(70);
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode=(StringNode)tmpList.get(24);
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode=(StringNode)tmpList.get(9);
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(76));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode=(StringNode)tmpList.get(27);
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode=(StringNode)tmpList.get(75);
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(76));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode=(StringNode)tmpList.get(49);
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(76));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode=(StringNode)tmpList.get(54);
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(9));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode=(StringNode)tmpList.get(72);
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode=(StringNode)tmpList.get(23);
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(76));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode=(StringNode)tmpList.get(9);
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode=(StringNode)tmpList.get(39);
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(9));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode=(StringNode)tmpList.get(66);
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode=(StringNode)tmpList.get(12);
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode=(StringNode)tmpList.get(60);
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode=(StringNode)tmpList.get(93);
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode=(StringNode)tmpList.get(80);
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode=(StringNode)tmpList.get(27);
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode=(StringNode)tmpList.get(58);
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode=(StringNode)tmpList.get(67);
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode=(StringNode)tmpList.get(85);
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode=(StringNode)tmpList.get(8);
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode=(StringNode)tmpList.get(78);
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode=(StringNode)tmpList.get(71);
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode=(StringNode)tmpList.get(90);
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(67));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode=(StringNode)tmpList.get(74);
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode=(StringNode)tmpList.get(33);
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode=(StringNode)tmpList.get(72);
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode=(StringNode)tmpList.get(22);
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode=(StringNode)tmpList.get(57);
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode=(StringNode)tmpList.get(73);
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode=(StringNode)tmpList.get(7);
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode=(StringNode)tmpList.get(23);
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode=(StringNode)tmpList.get(7);
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode=(StringNode)tmpList.get(30);
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode=(StringNode)tmpList.get(46);
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(9));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode=(StringNode)tmpList.get(40);
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(98));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode=(StringNode)tmpList.get(73);
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(33));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(34));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode=(StringNode)tmpList.get(30);
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(9));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode=(StringNode)tmpList.get(33);
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(0));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(84));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(12));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(89));
		tmpNode.linkTo((StringNode)tmpList.get(90));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode=(StringNode)tmpList.get(71);
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(56));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(29));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(50));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode=(StringNode)tmpList.get(40);
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(87));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(78));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(17));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(94));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode=(StringNode)tmpList.get(3);
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(24));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(11));
		tmpNode.linkTo((StringNode)tmpList.get(25));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode.linkTo((StringNode)tmpList.get(69));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(92));
		tmpNode.linkTo((StringNode)tmpList.get(57));
		tmpNode.linkTo((StringNode)tmpList.get(16));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(26));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode=(StringNode)tmpList.get(43);
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(30));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(61));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(45));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode=(StringNode)tmpList.get(29);
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(5));
		tmpNode.linkTo((StringNode)tmpList.get(22));
		tmpNode.linkTo((StringNode)tmpList.get(96));
		tmpNode.linkTo((StringNode)tmpList.get(14));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(21));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(1));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(76));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(74));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(39));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(3));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode=(StringNode)tmpList.get(44);
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(9));
		tmpNode.linkTo((StringNode)tmpList.get(58));
		tmpNode.linkTo((StringNode)tmpList.get(59));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(91));
		tmpNode.linkTo((StringNode)tmpList.get(82));
		tmpNode.linkTo((StringNode)tmpList.get(73));
		tmpNode.linkTo((StringNode)tmpList.get(66));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(13));
		tmpNode.linkTo((StringNode)tmpList.get(62));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(35));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(7));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(19));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode=(StringNode)tmpList.get(73);
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(4));
		tmpNode.linkTo((StringNode)tmpList.get(85));
		tmpNode.linkTo((StringNode)tmpList.get(52));
		tmpNode.linkTo((StringNode)tmpList.get(18));
		tmpNode.linkTo((StringNode)tmpList.get(40));
		tmpNode.linkTo((StringNode)tmpList.get(31));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(83));
		tmpNode.linkTo((StringNode)tmpList.get(99));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(77));
		tmpNode.linkTo((StringNode)tmpList.get(23));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(68));
		tmpNode.linkTo((StringNode)tmpList.get(72));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode=(StringNode)tmpList.get(23);
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(81));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(60));
		tmpNode.linkTo((StringNode)tmpList.get(63));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode.linkTo((StringNode)tmpList.get(32));
		tmpNode.linkTo((StringNode)tmpList.get(46));
		tmpNode.linkTo((StringNode)tmpList.get(6));
		tmpNode.linkTo((StringNode)tmpList.get(8));
		tmpNode.linkTo((StringNode)tmpList.get(95));
		tmpNode.linkTo((StringNode)tmpList.get(28));
		tmpNode.linkTo((StringNode)tmpList.get(49));
		tmpNode.linkTo((StringNode)tmpList.get(42));
		tmpNode.linkTo((StringNode)tmpList.get(37));
		tmpNode.linkTo((StringNode)tmpList.get(51));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(80));
		tmpNode.linkTo((StringNode)tmpList.get(64));
		tmpNode.linkTo((StringNode)tmpList.get(55));
		tmpNode.linkTo((StringNode)tmpList.get(47));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(10));
		tmpNode.linkTo((StringNode)tmpList.get(43));
		tmpNode.linkTo((StringNode)tmpList.get(93));
		tmpNode=(StringNode)tmpList.get(6);
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(36));
		tmpNode.linkTo((StringNode)tmpList.get(15));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(38));
		tmpNode.linkTo((StringNode)tmpList.get(41));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(97));
		tmpNode.linkTo((StringNode)tmpList.get(48));
		tmpNode.linkTo((StringNode)tmpList.get(27));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(70));
		tmpNode.linkTo((StringNode)tmpList.get(75));
		tmpNode.linkTo((StringNode)tmpList.get(53));
		tmpNode.linkTo((StringNode)tmpList.get(2));
		tmpNode.linkTo((StringNode)tmpList.get(65));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(44));
		tmpNode.linkTo((StringNode)tmpList.get(20));
		tmpNode.linkTo((StringNode)tmpList.get(71));
		tmpNode.linkTo((StringNode)tmpList.get(79));
		tmpNode.linkTo((StringNode)tmpList.get(88));
		tmpNode.linkTo((StringNode)tmpList.get(54));
		tmpNode.linkTo((StringNode)tmpList.get(86));
		tmpNode.linkTo((StringNode)tmpList.get(69));

		// calculate the paths
		ShortestPathFinder spf=new ShortestPathFinder();
		spf.calculatePaths(tmpList);
	}

	private class StringNode extends AbstractSPNode {
		private String str=null;

		public StringNode(String s) {
			super();
			if(s == null) {
				throw new NullPointerException("s can't be null");
			}
			str=s;
		}

		public String toString() {
			return("{StringNode " + str + "}");
		}

		public String getString() {
			return(str);
		}

		public void linkTo(StringNode x, int cost) {
			super.linkTo(x, cost);
		}

		public void linkTo(StringNode x) {
			super.linkTo(x);
		}

		private String indent(int indentation) {
			StringBuffer sb=new StringBuffer(indentation);
			for(int i=0; i<indentation; i++) {
				sb.append(' ');
			}
			return (sb.toString());
		}

		public void dump() {
			Set s=new java.util.HashSet();
			dump(0, s);
		}

		public void dump(int indentation, Set s) {
			System.out.println(indent(indentation) + this);
			// Find out what we have maps to
			for(Iterator i=nodes.values().iterator(); i.hasNext(); ) {
				StringNode sn=(StringNode)i.next();
				SPVertex vert=getNextHop(sn);
				if(vert != null) {
					System.out.println(indent(indentation + 4)
						+ " --> " + sn.getString() + " via "
						+ vert.getTo() + "@" + vert.getCost());
				}
			}
			// Dump out the connections
			for(Iterator i=getConnections().iterator(); i.hasNext(); ) {
				SPVertex spv=(SPVertex)i.next();
				if (! s.contains(spv.getTo())) {
					s.add(spv.getTo());
					StringNode sn=(StringNode)spv.getTo();
					sn.dump(indentation+2, s);
				}
			}
		}

		public int hashCode() {
			return(str.hashCode());
		}

		public int compareTo(Object o) {
			StringNode sn=(StringNode)o;

			return (str.compareTo(sn.getString()));
		}
	}

}
